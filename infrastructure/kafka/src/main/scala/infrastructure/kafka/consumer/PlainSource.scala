package infrastructure.kafka.consumer

import akka.Done
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import infrastructure.serialization.algebra.Deserializer
import infrastructure.kafka.consumer.logger.Protocol
import infrastructure.kafka.KafkaSupport.Protocol._

import scala.concurrent.Future

class PlainSource[A]()(implicit requirements: KafkaRequirements, deserializer: Deserializer[A]) {
  private implicit val actorSystem = requirements.actorSystem

  def run(topic: String, group: String)(callback: A => Either[String, Unit]): (UniqueKillSwitch, Future[Done]) = {
    val source = Consumer
      .plainSource(settings.Consumer.apply.withGroupId(group), Subscriptions.topics(topic))
      .viaMat(KillSwitches.single)(Keep.right)
      .map(_.value)

    def log: Protocol => Unit = requirements.logger.log

    source
      .map { msg =>
        val output = deserializer.deserialize(msg) match {
          case Left(_) => Protocol.`Failed to deserialize`(topic, msg)
          case Right(value) =>
            callback(value) match {
              case Left(_) => Protocol.`Failed to process`(topic, msg)
              case Right(_) => Protocol.`Processed`(topic, msg)
            }
        }
        log(output)
        output
      }
      .toMat(Sink.ignore)(Keep.both)
      .run()
  }

  def source(topic: String, group: String): Source[A, UniqueKillSwitch] = {
    val source: Source[String, UniqueKillSwitch] = Consumer
      .plainSource(infrastructure.kafka.consumer.settings.Consumer.apply.withGroupId(group),
                   Subscriptions.topics(topic))
      .viaMat(KillSwitches.single)(Keep.right)
      .map(_.value)

    source
      .map { msg =>
        deserializer.deserialize(msg)
      }
      .collect {
        case Right(value) => value
      }
  }
}
