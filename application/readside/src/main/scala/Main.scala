/*import akka.actor.ActorSystem
import domain.Bid
import infrastructure.kafka.consumer.logger.{Logger, Protocol}
import infrastructure.kafka.KafkaSupport.Protocol._
import infrastructure.kafka.KafkaSupport.Implicit._
import infrastructure.serialization.algebra.Deserializer
 */
object Main /*extends App {
  implicit lazy val actorSystem = ActorSystem("Readside")
  val logger: Logger = {
    case Protocol.`Failed to deserialize`(topic, msg) =>
      println(s"Failed to deserialize $topic $msg")
    case Protocol.`Failed to process`(topic, msg) =>
      println(s"Failed to process $topic $msg")
    case Protocol.`Processed`(topic, msg) =>
      println(s"Processed $topic $msg")
  }
  implicit val kafkaRequirements = KafkaRequirements(
    KafkaBootstrapServer("0.0.0.0:29092"),
    actorSystem,
    logger
  )

  domain.Bid.kafka.consumer.`commit`.run(
    "bid",
    "default"
  ) {
    case Bid(user, lot, 0) =>
      Left("Bid is zero")
    case Bid(user, lot, bid) =>
      Right(())

  }

}
 */
