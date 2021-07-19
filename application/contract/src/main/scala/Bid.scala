import akka.actor.typed.ActorRef
import infrastructure.tracing.Tracing

case class Topic[Message <: Tracing](name: String)

//Topic[Bid]("bid")
trait ProcessBid[Bid <: Tracing, ProcessBid] {
  type b = ActorRef[ProcessBid]
  type a = infrastructure.kafka.algebra.MessageProcessor[Int]
}
