package infrastructure.kafka

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import akka.stream.UniqueKillSwitch
import infrastructure.kafka.algebra.KafkaTransaction
import akka.http.scaladsl.server.Directives._
import infrastructure.kafka.Transaction.{Documentation, FromTo, UsefulUrls}
import infrastructure.serialization.interpreter.`JSON Serialization`
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
) {

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

  // format: off
    
    val grafanaRED = `RED Dashboard`(microserviceName, kafkaTransaction.from).asJson
    /* TODO
     make all Consumers and Producers have a name, aside from the topic,
     because you never know when someone is going to consume a topic two different times.
     the name, which is more of an ID, should be: s"${topic}-${consumerGroup}-${nodeId}"
   */
    lazy val routes: Route = get {

      path("") {
        complete {
          UsefulUrls serialize
            UsefulUrls(
              Seq.empty
            )
        }
      }
    } ~
      get { path("transaction" / transactionName / "documentation") {
        complete {
          Documentation serialize
          Documentation(Documentation.example.explanation,
          FromTo(
            from = kafkaTransaction.from,
            to = kafkaTransaction.to
          )
          )
        }
      }
    } ~  get {
      path("transaction" / transactionName / "grafana") {
        complete {
          grafanaRED
        }
      }
    } ~ get {
        path("transaction" / transactionName / "start") {
          complete {
            start
            "OK"
          }
        }
      } ~ get {
        path("transaction" / transactionName / "stop") {
          complete {
            start
            "OK"
          }
        }
      }
  }

object Transaction{

  import infrastructure.serialization.interpreter.`JSON Serialization`
  import play.api.libs.json.{Format, Json}

  case class UsefulUrls(
                         useful_urls: Seq[String]
                       )

  object UsefulUrls extends `JSON Serialization`[UsefulUrls] {
    val example = UsefulUrls( Seq.empty)
    val json: Format[UsefulUrls] = Json.format
  }

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
    val example = Documentation( "no explanation.", FromTo("topic A", "topic B"))
    val json: Format[Documentation] = Json.format
  }
  case class FromTo(from: String, to: String)
  object FromTo extends `JSON Serialization`[FromTo] {
    val example = FromTo("topic A", "topic B")
    val json: Format[FromTo] = Json.format
  }

}