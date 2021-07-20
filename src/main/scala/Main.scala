import Main.HelloProtocol.HelloCommand
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import infrastructure.actor.ShardedActor
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.microservice.{Microservice, TransactionMicroservice}
import infrastructure.serialization.interpreter.`JSON Serialization`
import infrastructure.transaction.Transaction
import infrastructure.transaction.interpreter.`object`.ObjectTransaction
import infrastructure.transaction.interpreter.free.FreeTransaction
import infrastructure.transaction.interpreter.functional.FunctionalTransaction
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

object Main extends App with Microservice with TransactionMicroservice {

  case class Bob(hi: String)
  object Bob extends `JSON Serialization`[Bob] {
    override val example = Bob(hi = "Hi Bob!")
    override implicit val json: Format[Bob] = Json.format
  }

  override val name = "Example"

  case class Hello(name: String)
  implicit object Hello extends `JSON Serialization`[Hello] {
    val example = Hello("Bob")
    val json: Format[Hello] = Json.format
  }
  import infrastructure.kafka.KafkaSupport.Implicit._

  sealed trait HelloProtocol
  object HelloProtocol {
    case class HelloCommand(hello: Hello, ref: ActorRef[Hello]) extends HelloProtocol
    case class BeHappy(id: String, actorRef: ActorRef[akka.Done]) extends HelloProtocol

  }
  case class AmountOfHappyPersons(amount: Int)

  object HappyPerson extends ShardedActor[HelloProtocol, AmountOfHappyPersons] {
    override protected def behavior(id: String)(state: Option[AmountOfHappyPersons]): Behavior[HelloProtocol] = {
      Behaviors.receiveMessage[HelloProtocol] {
        case HelloProtocol.HelloCommand(hello, ref) =>
          ref ! Hello(hello + " :)")
          //HappyPerson.behavior(id)(state map ( _.amount + 1) map HappyPerson)
          HappyPerson.behavior(id)(state.map(state => state.copy(amount = state.amount + 1)))
      }
    }

    def apply(id: String, state: Option[AmountOfHappyPersons] = None): Behavior[HelloProtocol] =
      behavior(id)(state)
  }

  sealed trait SadPersonCommand
  case class AmountOfSadPersons(amount: Int) extends SadPersonCommand
  object SadPersonCommand {
    case class BeHappy(id: String, actorRef: ActorRef[akka.Done]) extends SadPersonCommand
  }
  object SadPerson extends ShardedActor[HelloProtocol, AmountOfSadPersons] {
    override protected def behavior(id: String)(state: Option[AmountOfSadPersons]): Behavior[HelloProtocol] = {
      Behaviors.receiveMessage[HelloProtocol] {
        case HelloProtocol.HelloCommand(hello, ref) =>
          ref ! Hello(hello + " :(")
          //HappyPerson.behavior(id)(state map ( _.amount + 1) map HappyPerson)
          SadPerson.behavior(id)(state.map(state => state.copy(amount = state.amount + 1)))
        case HelloProtocol.BeHappy(id, ref) =>
          ref ! akka.Done
          HappyPerson.apply(id)
      }
    }
  }

  override def transactions: Set[Transaction] =
    `with transactionRequirements` { implicit transactionsRequirements =>
      import transactionsRequirements.Implicits._
      Set(
        Transaction(
          microserviceName = name,
          nodeIdentification = "0",
          consumerGroup = "example",
          transactionName = "Example Transaction",
          kafkaTransaction = FreeTransaction(
            "from",
            "to", { implicit ec =>
              val streamCompletion = Hello.kafka.consumer.plain.run("from", "example") { hello: Hello =>
                for {
                  response: Hello <- HappyPerson.ask(hello.name)(HelloCommand(hello, _))
                } yield println(response)
                Future.successful(Right(()))
              }
              (
                Some(streamCompletion._1),
                streamCompletion._2
              )

            }
          ),
          executionContext = system.dispatcher
        ),
        Transaction(
          microserviceName = name,
          nodeIdentification = "0",
          consumerGroup = "example",
          transactionName = "Example Transaction",
          kafkaTransaction = {
            implicit val domainToCommand: (Hello, ActorRef[Hello]) => HelloProtocol = HelloProtocol.HelloCommand.apply
            implicit val `Hello entityIdExtractor`: Hello => String = _.name
            type Domain = Hello
            type Command = HelloProtocol
            type Response = Hello
            type State = AmountOfHappyPersons
            ObjectTransaction[
              Domain,
              Command,
              Response,
              State
            ](
              HappyPerson,
              "from",
              "to"
            )
          },
          executionContext = system.dispatcher
        ),
        Transaction(
          microserviceName = name,
          nodeIdentification = "0",
          consumerGroup = "example",
          transactionName = "Example Transaction",
          kafkaTransaction = {
            FunctionalTransaction[Hello, Hello](
              { hello: Hello =>
                Future.successful(hello)
              },
              "from",
              "to"
            )
          },
          executionContext = system.dispatcher
        )
      )
    }

  serve
}
