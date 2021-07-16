package infrastructure.kafka.algebra

import scala.concurrent.{ExecutionContext, Future}
import akka.Done
import akka.actor.ActorSystem
import akka.stream.{KillSwitch, OverflowStrategy, UniqueKillSwitch}
import akka.stream.scaladsl.{Sink, Source, SourceQueue}

import scala.collection.mutable

trait MessageProcessor[Command] {

  def run(topic: String, group: String)(
      callback: Command => Future[Either[String, Unit]]
  ): (Option[UniqueKillSwitch], Future[Done])

}
object MessageProducer {
  case class KafkaKeyValue(aggregateRoot: String, json: String) {
    def key = aggregateRoot
    def value = json
  }
}
trait MessageProducer {
  def createTopic(topic: String): Future[Done]
  def produce(data: Seq[MessageProducer.KafkaKeyValue], topic: String)(
      handler: Seq[MessageProducer.KafkaKeyValue] => Unit
  ): Future[Done]
}
