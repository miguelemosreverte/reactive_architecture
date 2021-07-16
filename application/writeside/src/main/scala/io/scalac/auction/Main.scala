package io.scalac.auction

object Main /*
import akka.stream.scaladsl.{Sink, Source}
import akka.{actor, NotUsed}
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.{OverflowStrategy, UniqueKillSwitch}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import domain.Bid
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.kafka.consumer.TransactionalSource
import infrastructure.kafka.producer.TransactionalProducer
import infrastructure.serialization.algebra.{Deserializer, Serializer}
import infrastructure.serialization.interpreter.`JSON Serialization`
import io.scalac.auction.auction.Domain.AuctionId
import io.scalac.auction.auction.Protocol.Commands
import io.scalac.auction.auction.actor.{Actor => AuctionActor}
import io.scalac.auction.lot.actor.{Actor => LotActor}
import io.scalac.auction.auction.{Protocol => AuctionProtocol}
import io.scalac.auction.lot.Domain.{Lot, LotId}
import io.scalac.auction.lot.{Protocol => LotProtocol}
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration.DurationInt
import scala.util.Random

object Main extends App {

  implicit lazy val system: ActorSystem = ActorSystem(
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
  )

  import akka.actor.typed.scaladsl.AskPattern._
  import akka.actor.typed.scaladsl.adapter._
  import infrastructure.kafka.KafkaSupport.Implicit._

  implicit lazy val typedSystem = system.toTyped
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = 20.seconds
  implicit val serializer = domain.Bid
  implicit val kafkaRequirements =
    KafkaRequirements(KafkaBootstrapServer("0.0.0.0:29092"), system, println, startFromZero = true)

  implicit lazy val sharding = ClusterSharding.apply(system.toTyped)

  case class CreateAuction(id: String, lots: Set[Lot] = Set.empty) extends Aggregate {
    override type Response = CreatedAuction
  }
  implicit object CreateAuction extends `JSON Serialization`[CreateAuction] {
    val example = CreateAuction("Old Egypt Treasure")
    val json: Format[CreateAuction] = Json.format
  }

  case class CreatedAuction(id: String, lots: Set[Lot] = Set.empty) extends Aggregate {
    override type Response = CreatedAuctionValidated
  }
  implicit object CreatedAuction extends `JSON Serialization`[CreatedAuction] {
    val example = CreatedAuction("Old Egypt Treasure")
    val json: Format[CreatedAuction] = Json.format
  }

  case class CreatedAuctionValidated(id: String, rejected: Boolean, reason: Option[String] = None) extends Aggregate {
    override type Response = CreatedAuction
  }
  implicit object CreatedAuctionValidated extends `JSON Serialization`[CreatedAuctionValidated] {
    val example = CreatedAuctionValidated("Old Egypt Treasure", rejected = false)
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

  println(CreateAuction.serialize(CreateAuction.example))

  import ActorTransactionDTO.Implicits.KafkaTransaction._

  ActorTransactionDTO[CreateAuction](AuctionActor, "CreateAuction", "CreatedAuction").run
  ActorTransactionDTO[CreatedAuction](LotActor, "CreatedAuction", "CreatedAuctionValidated").run

}
 */
