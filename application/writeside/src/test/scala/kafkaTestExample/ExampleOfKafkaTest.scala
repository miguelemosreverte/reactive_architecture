package kafkaTestExample

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import infrastructure.actor.ShardedActor
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.kafka.algebra.KafkaTransaction
import infrastructure.kafka.interpreter.`object`.ObjectTransaction
import infrastructure.kafka.interpreter.functional.FunctionalTransaction
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ExampleOfKafkaTest extends AnyWordSpecLike with Matchers with Eventually with IntegrationPatience {
  import akka.actor.typed.scaladsl.adapter._

  implicit lazy val system = ActorSystem(
    "auctionspec",
    ConfigFactory.parseString("""
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
  |""".stripMargin)
  ).toTyped

  implicit lazy val sharding = ClusterSharding.apply(system)

  implicit val kafkaRequirements =
    KafkaRequirements(KafkaBootstrapServer("0.0.0.0:29092"), system.toClassic, println, startFromZero = true)
  implicit val ec = system.classicSystem.dispatcher
  implicit val timeout: Timeout = 20.seconds

  import kafkaTestExample.Commands._
  import kafkaTestExample.Domain._
  import kafkaTestExample.DomainToCommand._
  import kafkaTestExample.EntityExtractors._

  implicit object AuctionActor extends ShardedActor[CreateAuction_, Int] {
    override def behavior(id: String)(state: Option[Int] = None): Behavior[CreateAuction_] =
      Behaviors.receiveMessage { msg: CreateAuction_ =>
        msg.ref ! CreatedAuction(msg.payload.id, msg.payload.lots)
        Behaviors.same
      }
  }

  implicit object LotActor extends ShardedActor[CreatedAuction_, Int] {

    def notAuctioned(id: String): Behavior[CreatedAuction_] =
      Behaviors.receiveMessage { msg =>
        msg.ref ! CreatedAuctionValidated(msg.payload.id, rejected = false)
        auctioned(id)
      }

    def auctioned(id: String): Behavior[CreatedAuction_] =
      Behaviors.receiveMessage { msg =>
        msg.ref ! CreatedAuctionValidated(
          msg.payload.id,
          true,
          Some(s"The lot ${id} had already been auctioned before.")
        )
        Behaviors.same
      }

    override def behavior(id: String)(state: Option[Int] = None): Behavior[CreatedAuction_] =
      notAuctioned(id)
  }

  "AuctionActor" should {

    "add lot" in {
      import infrastructure.kafka.KafkaMock.Implicits._
      withSession { implicit testSession: TestSession =>
        val `Kafka Transactions`: Seq[KafkaTransaction] = Seq(
          ObjectTransaction(AuctionActor, "CreateAuction", "CreatedAuction"),
          ObjectTransaction(LotActor, "CreatedAuction", "CreatedAuctionValidated"),
          FunctionalTransaction({ a: CreatedAuctionValidated =>
            Future.successful(Done("hi"))
          }, "CreatedAuctionValidated", "End")
        )

        val `Events to Publish To Kafka`: Seq[(String, String)] = Seq(
          ("CreateAuction", CreateAuction.serialize(CreateAuction.example)),
          ("CreateAuction", CreateAuction.serialize(CreateAuction.example))
        )

        val `Kafka Expected Last Event` = CreatedAuctionValidated serialize CreatedAuctionValidated(
            id = "Old Egypt Treasure",
            rejected = true,
            reason = Some("The lot Old Egypt Treasure had already been auctioned before.")
          )

        `Kafka Transactions`.foreach {
          case kafkaTransaction: KafkaTransaction =>
            kafkaTransaction run
        }
        `Events to Publish To Kafka`.foreach {
          case (topic, event) => kafkaMockRequirements.publishToKafka(topic, event)
        }

        eventually(timeout(Span(6, Seconds))) {
          Thread.sleep(1000)
          kafkaMockRequirements
            .eventLog()
            .last should be(
            `Kafka Expected Last Event`
          )
        }
      }

    }
  }
}
