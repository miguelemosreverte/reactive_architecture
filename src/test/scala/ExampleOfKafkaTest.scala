import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import infrastructure.actor.ShardedActor
import infrastructure.actor.ShardedActor.{Aggregate, Message}
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.kafka.interpreter.`object`.ObjectTransaction
import infrastructure.kafka.interpreter.functional.FunctionalTransaction
import infrastructure.serialization.interpreter.`JSON Serialization`
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ExampleOfKafkaTest extends AnyWordSpecLike with Matchers with Eventually with IntegrationPatience {
  import akka.actor.typed.scaladsl.adapter._

  implicit lazy val system = ActorSystem(
    "auctionspec",
    ConfigFactory.parseString(
      """
                                |
                                |akka {
                                |  actor {
                                |    provider = cluster
                                |    debug {
                                |      receive = on
                                |    }
                                |  }
                                |
                                |  remote {
                                |    artery {
                                |      transport = tcp # See Selecting a transport below
                                |      canonical.hostname = "0.0.0.0"
                                |      canonical.port = 2552
                                |    }
                                |  }
                                |
                                |  cluster {
                                |    seed-nodes = ["akka://auctionspec@0.0.0.0:2552"]
                                |  }
                                |
                                |  extensions = ["akka.cluster.pubsub.DistributedPubSub"]
                                |
                                |
                                |}
                                |""".stripMargin
    )
  ).toTyped

  implicit lazy val sharding = ClusterSharding.apply(system)

  implicit val kafkaRequirements =
    KafkaRequirements(
      KafkaBootstrapServer("0.0.0.0:29092"),
      system.toClassic,
      println,
      startFromZero = true
    )
  implicit val ec = system.classicSystem.dispatcher
  implicit val timeout: Timeout = 20.seconds

  case class CreateAuction(id: String, lots: Set[String] = Set.empty) extends Aggregate {
    override type Response = CreatedAuction
  }
  implicit object CreateAuction extends `JSON Serialization`[CreateAuction] {
    val example = CreateAuction("Old Egypt Treasure", Set.empty)
    val json: Format[CreateAuction] = Json.format
  }

  case class CreatedAuction(id: String, lots: Set[String] = Set.empty) extends Aggregate {
    override type Response = CreatedAuctionValidated
  }
  implicit object CreatedAuction extends `JSON Serialization`[CreatedAuction] {
    val example = CreatedAuction("Old Egypt Treasure", Set.empty)
    val json: Format[CreatedAuction] = Json.format
  }

  case class CreatedAuctionValidated(
      id: String,
      rejected: Boolean,
      reason: Option[String] = None
  ) extends Aggregate {
    override type Response = Done
  }
  implicit object CreatedAuctionValidated extends `JSON Serialization`[CreatedAuctionValidated] {
    val example =
      CreatedAuctionValidated("Old Egypt Treasure", rejected = false, None)
    val json: Format[CreatedAuctionValidated] = Json.format
  }

  implicit object AuctionActor extends ShardedActor[CreateAuction] {
    override def behavior(id: String): `Command to Event` =
      Behaviors.receiveMessage { msg: Message[CreateAuction, CreateAuction#Response] =>
        msg.ref ! CreatedAuction(msg.command.id, msg.command.lots)
        Behaviors.same
      }
  }

  implicit object LotActor extends ShardedActor[CreatedAuction] {

    def notAuctioned(id: String): `Command to Event` =
      Behaviors.receiveMessage { msg: Message[CreatedAuction, CreatedAuction#Response] =>
        msg.ref ! CreatedAuctionValidated(msg.command.id, rejected = false)
        auctioned(id)
      }

    def auctioned(id: String): `Command to Event` =
      Behaviors.receiveMessage { msg: Message[CreatedAuction, CreatedAuction#Response] =>
        msg.ref ! CreatedAuctionValidated(
          msg.command.id,
          true,
          Some(s"The lot ${id} had already been auctioned before.")
        )
        Behaviors.same
      }

    override def behavior(id: String): `Command to Event` =
      notAuctioned(id)
  }

  case class Done(done: String = "Done")
  implicit object DoneSerializer extends `JSON Serialization`[Done] {
    val example = Done("hi")
    override implicit val json: Format[Done] = Json.format
  }

  "AuctionActor" should {

    "add lot" in {
      import infrastructure.kafka.KafkaMock.Implicits._
      withSession { implicit testSession: TestSession =>
        ObjectTransaction[CreateAuction](
          AuctionActor,
          "CreateAuction",
          "CreatedAuction"
        ).run
        ObjectTransaction[CreatedAuction](
          LotActor,
          "CreatedAuction",
          "CreatedAuctionValidated"
        ).run
        FunctionalTransaction[CreatedAuctionValidated](
          { a: CreatedAuctionValidated =>
            Future.successful(Done("hi"))
          },
          "CreatedAuctionValidated",
          "End"
        ).run

        kafkaMockRequirements.publishToKafka(
          "CreateAuction",
          CreateAuction.serialize(CreateAuction.example)
        )
        kafkaMockRequirements.publishToKafka(
          "CreateAuction",
          CreateAuction.serialize(CreateAuction.example)
        )

        eventually(timeout(Span(6, Seconds))) {
          Thread.sleep(1000)
          kafkaMockRequirements
            .eventLog() should contain(
            CreatedAuctionValidated serialize CreatedAuctionValidated(
              id = "Old Egypt Treasure",
              rejected = true,
              reason = Some(
                "The lot Old Egypt Treasure had already been auctioned before."
              )
            )
          )
        }
      }

    }
  }
}
