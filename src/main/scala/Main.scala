import akka.actor.ActorSystem
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

object Main extends App {

  case class Bob(hi: String)
  object Bob extends `JSON Serialization`[Bob] {
    override val example = Bob(hi = "Hi Bob!")
    override implicit val json: Format[Bob] = Json.format
  }
  implicit val system = ActorSystem("")
  import infrastructure.kafka.KafkaSupport.Implicit._
  implicit val kafkaRequirements = KafkaRequirements(KafkaBootstrapServer("0.0.0.0: 29092"), system, println)
  val fromKafka = Bob.kafka.consumer.transactional.run("topic", "group"){ bob =>
    Future.successful(Right())
  }

  fromKafka.
}
