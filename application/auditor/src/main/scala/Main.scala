import infrastructure.http.Client.GET
import infrastructure.kafka.Transaction
import infrastructure.kafka.algebra.KafkaTransaction
import infrastructure.microservice.cake.withHttpClient
import infrastructure.microservice.{Microservice, TransactionMicroservice}
import infrastructure.serialization.algebra.Serialization
import infrastructure.serialization.interpreter.`JSON Serialization`
import infrastructure.tracing.detection.stoppages.DetectionOfUnconfirmedMessages
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration.DurationInt
import scala.language.{implicitConversions, postfixOps}

object Main extends App with TransactionMicroservice with withHttpClient {

  val name = "Auditor"
  //implicit val ec = system.dispatcher

  case class Bob(hi: String)
  implicit object Bob extends `JSON Serialization`[Bob] {
    val example = Bob("hi")
    val json: Format[Bob] = Json.format
  }

  lazy val transactions: Set[Transaction] =
    `with transactionRequirements` { implicit transactionRequirements =>
      implicit val ec = transactionRequirements.executionContext

      import infrastructure.http.Client.Implicits._
      import infrastructure.kafka.Transaction.FromTo
      implicit val `FromTo deserializer`: Serialization[FromTo] = infrastructure.kafka.Transaction.FromTo

      lazy val done = GET.apply[FromTo]("0.0.0.0:8081/topics").response

      Seq(
        ("bid", "bidded"),
        ("lot", "lot_created")
      ).map {
          case (from, to) =>
            DetectionOfUnconfirmedMessages(
              println,
              measures = Seq(
                10 seconds,
                30 seconds,
                60 seconds
              ),
              from,
              to
            )
        }
        .flatMap {
          case DetectionOfUnconfirmedMessages.Signature(pendingStream, confirmationStream) =>
            def toTransaction: KafkaTransaction => Transaction =
              kafkaTransaction =>
                Transaction(
                  microserviceName = name,
                  consumerGroup = name,
                  nodeIdentification = "1",
                  transactionName = kafkaTransaction.from,
                  kafkaTransaction = kafkaTransaction,
                  transactionRequirements.executionContext
                )
            Seq(
              toTransaction(pendingStream),
              toTransaction(confirmationStream)
            )

        }
        .toSet
    }

  serve
}
