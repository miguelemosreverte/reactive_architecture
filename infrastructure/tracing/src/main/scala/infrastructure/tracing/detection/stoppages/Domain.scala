package infrastructure.tracing.detection.stoppages

import akka.actor.typed.ActorRef
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration.Duration

object Domain {

  trait ActorMessages

  case class Message(payload: ActorMessages, ref: ActorRef[akka.Done])

  case class Confirmation(id: String, fromId: String, timestampOfArrival: Long) extends ActorMessages {
    val sendTo = fromId
  }

  case class PendingConfirmation(id: String, fromId: String, timestampOfArrival: Long) extends ActorMessages {
    val sendTo = id
  }

  case class Timeout(id: String, timeInQueue: Duration) extends ActorMessages

  implicit object Confirmation extends `JSON Serialization`[Confirmation] {
    override def example: Confirmation = Confirmation("1", "2", 1L)

    override implicit val json: Format[Confirmation] = Json.format
  }

  implicit object PendingConfirmation extends `JSON Serialization`[PendingConfirmation] {
    override def example: PendingConfirmation = PendingConfirmation("1", "2", 1L)

    override implicit val json: Format[PendingConfirmation] = Json.format
  }
}
