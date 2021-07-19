package infrastructure.microservice

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.{get, path, _}
import akka.http.scaladsl.server.Route
import infrastructure.http.Server
import infrastructure.kafka.Transaction
import infrastructure.kafka.interpreter.`object`.ObjectTransaction.Implicits.KafkaTransaction.KafkaTransactionRequirements
import infrastructure.microservice.TransactionMicroservice.{Documentation, Transactions}
import infrastructure.microservice.cake.{withKafkaRequirements, withMetrics, withSystem}
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

trait TransactionMicroservice
    extends Microservice
    with withMetrics
    with withSystem
    with withKafkaRequirements
    with Server {

  def transactions: Set[Transaction]

  protected def `with transactionRequirements`(
      andThen: KafkaTransactionRequirements => Set[Transaction]
  ): Set[Transaction] =
    andThen(KafkaTransactionRequirements(clusterSharding, typedSystem, kafkaRequirements, system.dispatcher))

  val explanation: String =
    """
      |This is a microservice that is focused on making Transactions:
      |
      |It reads from Kafka topics, processes, and outputs to Kafka, with exactly-once delivery guarantees.
      |
      |""".stripMargin

  val preffix: String = ""
  override protected def selfPath = s"$interface:$port"

  override val usefulUrls: Seq[String] = super.usefulUrls ++ Seq(
      s"$selfPath/transactions"
    )

  def getTransactions: String =
    Transactions serialize
    Transactions(
      transactions.map(_.transactionName).toSeq.map(name => s"${selfPath}/transaction/${name}")
    )

  override def examples: Seq[Example] = Seq(
    `GET Example`("transactions", getTransactions)
  )

  override def routes: Route = {
    super.routes ~ (Seq(
      get {
        path("transactions") {
          complete {
            getTransactions

          }
        }
      }
    ) ++ transactions.map(_.routes)).reduce(_ ~ _)
  }

  def serve = Server.apply(routes, interface, port)

}

object TransactionMicroservice {

  case class Documentation(
      useful_urls: Seq[String]
  )
  object Documentation extends `JSON Serialization`[Documentation] {
    val example = Documentation(Seq.empty)
    val json: Format[Documentation] = Json.format
  }
  case class Transactions(transactions: Seq[String])
  object Transactions extends `JSON Serialization`[Transactions] {
    val example = Transactions(Seq.empty)
    val json: Format[Transactions] = Json.format
  }
}
