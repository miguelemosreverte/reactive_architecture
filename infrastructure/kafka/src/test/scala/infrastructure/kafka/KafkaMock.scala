package infrastructure.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import infrastructure.actor.ShardedActor.Aggregate
import infrastructure.kafka.algebra.{MessageProcessor, MessageProducer}
import infrastructure.serialization.algebra._
import org.scalatest.Assertion

import java.util.UUID
import scala.concurrent.Future
import scala.reflect.ClassTag

object KafkaMock {
  case class SubscribeTo(topic: String, receiveMessages: String => Unit)

  object Implicits {

    def withSession(then: TestSession => Assertion) = {
      implicit val testSession = TestSession()
      then(testSession)
    }
    case class TestSession(id: UUID = UUID.randomUUID())
    var onePerSession: collection.mutable.Map[UUID, KafkaMockRequirements] = collection.mutable.Map.empty

    implicit def kafkaMockRequirements(
        implicit
        system: ActorSystem[_],
        onlyOnePerTestSession: TestSession
    ): KafkaMockRequirements = {
      onePerSession.get(onlyOnePerTestSession.id) match {
        case Some(hereYouGo) => hereYouGo
        case None =>
          onePerSession.addOne(onlyOnePerTestSession.id, KafkaMockRequirements.apply())
          kafkaMockRequirements
      }
    }

    implicit def toMock[Command <: Aggregate: ClassTag](
        implicit
        commandSerialization: Serialization[Command],
        responseSerialization: Serialization[Command#Response],
        kafkaMockRequirements: KafkaMockRequirements,
        system: ActorSystem[_]
    ) = new KafkaMock[Command]()
  }
}
class KafkaMock[Command <: Aggregate: ClassTag](
    )(
    implicit
    kafkaMockRequirements: KafkaMockRequirements,
    s: Serialization[Command],
    ss: Serialization[Command#Response],
    system: ActorSystem[Nothing]
) extends MessageProducer[Command]
    with MessageProcessor[Command] {

  override def producer(topic: String): SourceQueue[Command#Response] =
    Source
      .queue(bufferSize = 1024, OverflowStrategy.backpressure)
      .to(
        Flow[Command#Response]
          .map(ss.serialize(_))
          .to(Sink.foreach {
            kafkaMockRequirements.onMessage(topic)
          })
      )
      .run

  override def run(topic: String, group: String)(
      callback: Command => Future[Either[String, Unit]]
  ): (Option[UniqueKillSwitch], Future[Done]) = {
    kafkaMockRequirements.receiveMessagesFrom(KafkaMock.SubscribeTo(topic, { message: String =>
      s deserialize message match {
        case Left(value) => Future.successful(Left(value.explanation))
        case Right(value) => callback(value)
      }
      ()
    }))
    (None, Future.successful(Done))
  }

}
