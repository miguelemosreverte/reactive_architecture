package infrastructure.kafka.consumer

import akka.Done
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerMessage, ProducerMessage, Subscriptions}
import akka.kafka.scaladsl.Transactional
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import infrastructure.serialization.algebra.{Deserializer, Serializer}
import infrastructure.kafka.consumer.logger.Protocol
import infrastructure.kafka.consumer.logger.Protocol._
import org.apache.kafka.clients.producer.ProducerRecord
import infrastructure.kafka.KafkaSupport.Protocol._
import scala.concurrent.Future

class TransactionalSource[A]()(implicit requirements: KafkaRequirements, deserializer: Deserializer[A]) {
  private implicit val actorSystem = requirements.actorSystem

  def run(topic: String, group: String)(callback: A => Either[String, Unit]): (UniqueKillSwitch, Future[Done]) = {

    val `topic to commit in case of errors` = s"${topic}_transactional_error"
    val `topic to commit in case of deserialization error` = s"${topic}_transactional_deserialization_error"
    val `topic to commit in case of success` = s"${topic}_transactional_success"

    val source = Transactional
      .source(settings.Consumer.apply.withGroupId(group), Subscriptions.topics(topic))
      .viaMat(KillSwitches.single)(Keep.right)

    def log: Protocol => Unit =
      requirements.logger.log

    def commit(
        msg: ConsumerMessage.TransactionalMessage[String, String]
    )(output: Protocol): ProducerMessage.Envelope[String, String, ConsumerMessage.PartitionOffset] = {
      ProducerMessage.single(
        new ProducerRecord(
          output match {
            case _: `Failed to deserialize` => `topic to commit in case of deserialization error`
            case _: `Failed to process` => `topic to commit in case of errors`
            case _: `Processed` => `topic to commit in case of success`
          },
          msg.record.key,
          msg.record.value
        ),
        msg.partitionOffset
      )
    }

    source
      .map { (msg: ConsumerMessage.TransactionalMessage[String, String]) =>
        val output = deserializer
          .deserialize(msg.record.value) match {
          case Left(_) =>
            Protocol.`Failed to deserialize`(topic, msg.record.value)
          case Right(value) =>
            callback(value) match {
              case Left(_) =>
                Protocol.`Failed to process`(topic, msg.record.value)
              case Right(_) =>
                Protocol.`Processed`(topic, msg.record.value)
            }
        }
        log(output)
        commit(msg)(output)
      }
      .via(Transactional.flow(infrastructure.kafka.producer.settings.Producer.apply, "transactionalId"))
      .toMat(Sink.ignore)(Keep.both)
      .run()

  }
}
