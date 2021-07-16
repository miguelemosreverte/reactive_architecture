import scala.concurrent.{ExecutionContext, Future}
import akka.Done
import akka.actor.ActorSystem
import akka.stream.{KillSwitch, OverflowStrategy, UniqueKillSwitch}
import akka.stream.scaladsl.{Sink, Source, SourceQueue}
import infrastructure.kafka.algebra.MessageProducer.KafkaKeyValue
import infrastructure.kafka.algebra.{MessageProcessor, MessageProducer}

import scala.collection.mutable

class KafkaMock() extends MessageProducer with MessageProcessor with MessageProcessorLogging {

  object PubSub {
    case class Message(topic: String, message: MessageProducer.KafkaKeyValue)
    case class SubscribeMe(topic: String, algorithm: String => Future[Seq[String]])
  }
  import PubSub._
  var subscriptors: Set[SubscribeMe] = Set.empty

  def receive(message: Any): Any = message match {
    case m: Message =>
      messageHistory = messageHistory :+ ((m.topic, m.message.json))
      println(
        s"""
           |${Console.YELLOW} [MessageProducer] ${Console.RESET}
           |Sending message to: ${subscriptors
             .filter(_.topic == m.topic)
             .map(_.topic)
             .map(Console.YELLOW + _ + Console.RESET)
             .mkString(",")}
           |${Console.CYAN} $message ${Console.RESET}
           |""".stripMargin
      )
      subscriptors.filter(_.topic == m.topic).foreach {
        _.algorithm(m.message.json)
      }
    case s: SubscribeMe =>
      subscriptors = subscriptors + s
  }

  override def produce(data: Seq[KafkaKeyValue], topic: String)(handler: Seq[KafkaKeyValue] => Unit) = {
    data map {
      PubSub.Message(topic, _)
    } foreach { receive }
    handler(data)
    Future.successful(Done)
  }

  override def run(SOURCE_TOPIC: String, SINK_TOPIC: String, algorithm: String => Future[Seq[String]]) =
    (None, {
      receive(PubSub.SubscribeMe(SOURCE_TOPIC, algorithm))
      Future.successful(Done)
    })

}

object KafkaMock {
  /*
  implicit class MessageProcessorImplicits(messageConsumer: MessageProcessor) {
    def subscribeActorTransaction(SOURCE_TOPIC: String, actorTransaction: ActorTransaction[_])(
        implicit ec: ExecutionContext
    ): (_, Future[Done]) =
      messageConsumer match {

        case processor: KafkaTransactionalMessageProcessor =>
          processor.run(SOURCE_TOPIC,
                        SOURCE_TOPIC + "_done",
                        message => actorTransaction.transaction(message).map(_ => Seq("Done")))

        case kafkaMock: KafkaMock =>
          (Done, {
            kafkaMock.receive(
              kafkaMock.PubSub.SubscribeMe(SOURCE_TOPIC,
                                           message => actorTransaction.transaction(message).map(_ => Seq("Done")))
            )
            Future(Done)
          })
        case _ =>
          (Done, Future(Done))
      }
  }*/
}
