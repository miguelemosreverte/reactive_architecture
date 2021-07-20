import DownloadImage.GetImage
import akka.actor.typed.ActorRef
import infrastructure.microservice.Microservice
import infrastructure.microservice.cake.{withHttpClient, withSystem}
import infrastructure.tracing.Tracing

case class Topic[Message <: Tracing](name: String)

//Topic[Bid]("bid")
trait ProcessBid[Bid <: Tracing, ProcessBid] {
  type b = ActorRef[ProcessBid]
  type a = infrastructure.kafka.algebra.MessageProcessor[Int]
}

object Pepe extends App with Microservice with withSystem with withHttpClient {
  val name = "Grafana"
  implicit val ec = system.dispatcher

  (1 to 100) foreach (i => new DownloadImage(GetImage(), s"$i.png"))

}
