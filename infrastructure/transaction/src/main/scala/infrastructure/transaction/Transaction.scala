package infrastructure.transaction

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.{Directive, PathMatcher, Route}
import akka.stream.UniqueKillSwitch
import akka.http.scaladsl.server.Directives._
import Transaction._
import akka.http.scaladsl.model.Uri.Path
import infrastructure.http.{`GET Example`, Example, Server}
import infrastructure.serialization.interpreter.`JSON Serialization`
import infrastructure.transaction.algebra.KafkaTransaction
import play.api.libs.json.{Format, Json}
import stages.`RED Dashboard`

import scala.concurrent.{ExecutionContext, Future}

case class Transaction(
    microserviceName: String,
    consumerGroup: String,
    nodeIdentification: String,
    transactionName: String,
    kafkaTransaction: KafkaTransaction,
    executionContext: ExecutionContext
)(
    implicit
    actorSystem: ActorSystem
) extends Server {

  private sealed trait Protocol

  private case class Start() extends Protocol

  private case class Stop() extends Protocol

  private object LifecycleController {
    private type Started = (Option[UniqueKillSwitch], Future[akka.Done])

    def apply(done: Option[Started] = None)(
        implicit
        kafkaTransaction: KafkaTransaction,
        executionContext: ExecutionContext
    ): Behavior[Protocol] =
      Behaviors.receiveMessage[Protocol] {
        case Start() if done.isEmpty =>
          apply(Some(kafkaTransaction.run))
        case Stop() if done.isDefined =>
          done foreach {
            case (Some(killswitch: UniqueKillSwitch), _) =>
              killswitch.shutdown()
            case _ => ()
          }
          apply(None)
        case _ =>
          Behaviors.same
      }
  }

  import akka.actor.typed.scaladsl.adapter._

  private val lifecycleController: ActorRef[Protocol] = {
    implicit val (k, ec) = (kafkaTransaction, executionContext)
    actorSystem.spawn(LifecycleController.apply(), transactionName)
  }

  def start: Unit = lifecycleController tell Start()

  def stop: Unit = lifecycleController tell Stop()

  import stages.add_dashboards._
  import stages.add_dashboards.Implicits._

  override def examples: Seq[Example] = Seq(
    `GET Example`(s"$selfPath/stop", "OK"),
    `GET Example`(s"$selfPath/start", "OK"),
    `GET Example`(s"$selfPath/grafana", grafanaRED),
    `GET Example`(s"$selfPath/documentation", documentation)
  )
  val documentation = Documentation serialize
    Documentation(Documentation.example.explanation,
                  FromTo(
                    from = kafkaTransaction.from,
                    to = kafkaTransaction.to
                  ))
  val grafanaRED = `RED Dashboard`(microserviceName, kafkaTransaction.from).asJson
  /* TODO
     make all Consumers and Producers have a name, aside from the topic,
     because you never know when someone is going to consume a topic two different times.
     the name, which is more of an ID, should be: s"${topic}-${consumerGroup}-${nodeId}"
   */

  override def usefulUrls: Seq[String] = Seq(
    s"$selfPath/start",
    s"$selfPath/stop",
    s"$selfPath/grafana",
    s"$selfPath/documentation"
  )

  override val preffix: Directive[Unit] = pathPrefix("transactions" / transactionName)
  override val preffixString: String = s"transactions/$transactionName"
  override def routes: Route =
    super.routes ~
    preffix {
      get {
        path("documentation") {
          complete {
            documentation
          }
        }
      } ~ get {
        path("grafana") {
          complete {
            grafanaRED
          }
        }
      } ~ get {
        path("start") {
          complete {
            start
            "OK"
          }
        }
      } ~ get {
        path("stop") {
          complete {
            start
            "OK"
          }
        }

      }
    }

  override val explanation: String =
    """
      |
      |This is the component in charge of making Transactions:
      |
      |It reads from Kafka topics, processes, and outputs to Kafka, with exactly-once delivery guarantees.
      |
      |""".stripMargin
}

object Transaction {

  import infrastructure.serialization.interpreter.`JSON Serialization`
  import play.api.libs.json.{Format, Json}

  case class Help(
      help: String
  )

  object Help extends `JSON Serialization`[Help] {
    val example = Help("")
    val json: Format[Help] = Json.format
  }

  case class Documentation(
      explanation: String,
      fromTo: FromTo
  )
  object Documentation extends `JSON Serialization`[Documentation] {
    val example = Documentation("no explanation.", FromTo("topic A", "topic B"))
    val json: Format[Documentation] = Json.format
  }
  case class FromTo(from: String, to: String)
  object FromTo extends `JSON Serialization`[FromTo] {
    val example = FromTo("topic A", "topic B")
    val json: Format[FromTo] = Json.format
  }

}
