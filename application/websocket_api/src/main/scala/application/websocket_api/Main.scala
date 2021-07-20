package application.websocket_api

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow
import infrastructure.http.Server
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.kafka.websocket.KafkaWebsocket

import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.util.Random
object Main extends App {

  implicit val system: ActorSystem = ActorSystem("broadcast")
  implicit val ec: ExecutionContext = system.dispatcher

  //kafka setup
  implicit val kafkaRequirements = KafkaRequirements(
    KafkaBootstrapServer("0.0.0.0:29092"),
    actorSystem = system,
    logger = logMe => println(logMe)
  )

  implicit val b = domain.Bid

  import akka.http.scaladsl.server.Directives._
  import infrastructure.kafka.KafkaSupport.Implicit._
  val routes: Flow[HttpRequest, HttpResponse, NotUsed] = {
    get {
      path("ws" / "room" / Segment) { topic =>
        KafkaWebsocket
          .apply[domain.Bid](
            ffromKafka = domain.Bid.kafka.consumer.plain.source(topic, "websocket_endpoint_" + Random.nextString(10)),
            ttoKafka = domain.Bid.kafka.producer.plain.sink(topic)
          )
          .route
      }
    }
  }

  Server(routes, "0.0.0.0", 8080)

}
