package infrastructure.kafka.consumer

import akka.Done
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.ProducerMessage.Envelope
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerMessage, ProducerMessage, Subscriptions}
import akka.kafka.scaladsl.{Committer, Consumer, Producer}
import akka.stream.scaladsl.Keep
import akka.stream.{KillSwitches, UniqueKillSwitch}
import infrastructure.serialization.algebra.Deserializer
import infrastructure.kafka.consumer.logger.Protocol
import infrastructure.kafka.consumer.logger.Protocol.{`Failed to deserialize`, `Failed to process`, `Processed`}
import org.apache.kafka.clients.producer.ProducerRecord
import infrastructure.kafka.KafkaSupport.Protocol._
import scala.concurrent.Future

class CommitableSource[A]()(implicit requirements: KafkaRequirements, deserializer: Deserializer[A]) {
  private implicit val actorSystem = requirements.actorSystem

  def run(topic: String, group: String)(callback: A => Either[String, Unit]): (UniqueKillSwitch, Future[Done]) = {

    val `topic to commit in case of success` = s"${topic}_transactional_success"

    def log: Protocol => Unit = requirements.logger.log
    def commit(
        msg: ConsumerMessage.CommittableMessage[String, String]
    )(output: Protocol): Envelope[String, String, CommittableOffset] =
      output match {
        case _: `Failed to deserialize` =>
          ProducerMessage.passThrough(msg.committableOffset)

        case _: `Failed to process` =>
          ProducerMessage.passThrough(msg.committableOffset)

        case _: `Processed` =>
          ProducerMessage.single(
            new ProducerRecord(
              `topic to commit in case of success`,
              msg.record.key,
              msg.record.value
            ),
            msg.committableOffset
          )
      }

    Consumer
      .committableSource(settings.Consumer.apply.withGroupId(group), Subscriptions.topics(topic))
      .viaMat(KillSwitches.single)(Keep.right)
      .map((msg: ConsumerMessage.CommittableMessage[String, String]) => {
        val output = deserializer.deserialize(msg.record.value) match {
          case Left(value) =>
            Protocol.`Failed to deserialize`(topic, msg.record.value)
          case Right(value) =>
            callback(value) match {
              case Left(_) =>
                Protocol.`Failed to deserialize`(topic, msg.record.value)
              case Right(_) =>
                Protocol.`Processed`(topic, msg.record.value)
            }
        }
        log(output)
        commit(msg)(output)
      })
      .via(Producer.flexiFlow(infrastructure.kafka.producer.settings.Producer.apply))
      .map(_.passThrough)
      .toMat(Committer.sink(akka.kafka.CommitterSettings.apply(actorSystem)))(Keep.both)
      .run()
  }
}
