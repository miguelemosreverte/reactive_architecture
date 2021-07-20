package io.scalac.auction

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
import infrastructure.actor.ShardedActor
import infrastructure.http.Client.GET
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.microservice.TransactionMicroservice
import infrastructure.serialization.algebra.{Deserializer, Serialization, Serializer}
import infrastructure.serialization.interpreter.`JSON Serialization`
import infrastructure.tracing.detection.stoppages.DetectionOfUnconfirmedMessages
import infrastructure.transaction.Transaction
import infrastructure.transaction.Transaction.FromTo
import infrastructure.transaction.algebra.KafkaTransaction
import infrastructure.transaction.interpreter.`object`.ObjectTransaction
import infrastructure.transaction.interpreter.free.FreeTransaction
import io.scalac.auction.auction.Domain.AuctionId
import io.scalac.auction.auction.Protocol.Commands
import io.scalac.auction.auction.actor.{Actor => AuctionActor}
import io.scalac.auction.lot.actor.{Actor => LotActor}
import io.scalac.auction.auction.{Protocol => AuctionProtocol}
import io.scalac.auction.lot.Domain.{Lot, LotId}
import io.scalac.auction.lot.{Protocol => LotProtocol}
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

object Main extends App with TransactionMicroservice {

  val name = "Writeside"

  implicit val serializer = domain.Bid

  case class CreateAuction_(payload: CreateAuction, ref: ActorRef[CreatedAuction])
  case class CreateAuction(id: String, lots: Set[Lot] = Set.empty)
  implicit object CreateAuction extends `JSON Serialization`[CreateAuction] {
    val example = CreateAuction("Old Egypt Treasure")
    val json: Format[CreateAuction] = Json.format
  }

  case class CreatedAuction_(payload: CreatedAuction, ref: ActorRef[CreatedAuctionValidated])
  case class CreatedAuction(id: String, lots: Set[Lot] = Set.empty)
  implicit object CreatedAuction extends `JSON Serialization`[CreatedAuction] {
    val example = CreatedAuction("Old Egypt Treasure")
    val json: Format[CreatedAuction] = Json.format
  }

  case class CreatedAuctionValidated_(payload: CreatedAuctionValidated, ref: ActorRef[akka.Done])
  case class CreatedAuctionValidated(id: String, rejected: Boolean, reason: Option[String] = None)
  implicit object CreatedAuctionValidated extends `JSON Serialization`[CreatedAuctionValidated] {
    val example = CreatedAuctionValidated("Old Egypt Treasure", rejected = false)
    val json: Format[CreatedAuctionValidated] = Json.format
  }

  implicit object AuctionActor extends ShardedActor[CreateAuction_, Int] {
    override def behavior(id: String)(state: Option[Int] = None): Behavior[CreateAuction_] =
      Behaviors.receiveMessage { msg: CreateAuction_ =>
        msg.ref ! CreatedAuction(msg.payload.id, msg.payload.lots)
        Behaviors.same
      }
  }

  implicit object LotActor extends ShardedActor[CreatedAuction_, Int] {

    def notAuctioned(id: String)(state: Option[Int] = None): Behavior[CreatedAuction_] =
      Behaviors.receiveMessage { msg: CreatedAuction_ =>
        msg.ref ! CreatedAuctionValidated(msg.payload.id, rejected = false)
        auctioned(id)(state)
      }

    def auctioned(id: String)(state: Option[Int] = None): Behavior[CreatedAuction_] =
      Behaviors.receiveMessage { msg: CreatedAuction_ =>
        msg.ref ! CreatedAuctionValidated(
          msg.payload.id,
          true,
          Some(s"The lot ${id} had already been auctioned before.")
        )
        Behaviors.same
      }

    override def behavior(id: String)(state: Option[Int] = None): Behavior[CreatedAuction_] =
      notAuctioned(id)()
  }

  println(CreateAuction.serialize(CreateAuction.example))

  lazy val transactions: Set[Transaction] = {
    `with transactionRequirements` { implicit transactionRequirements =>
      import transactionRequirements.Implicits._
      implicit val executionContext = transactionRequirements.executionContext

      implicit val `CreateAuction domainToCommand`: (CreateAuction, ActorRef[CreatedAuction]) => CreateAuction_ =
        CreateAuction_.apply
      implicit val `CreateAuction  domainEntityIdExtractor`: CreateAuction => String = _.id

      implicit val `CreatedAuction domainToCommand`
          : (CreatedAuction, ActorRef[CreatedAuctionValidated]) => CreatedAuction_ =
        CreatedAuction_.apply
      implicit val `CreatedAuction  domainEntityIdExtractor`: CreatedAuction => String = _.id

      Set(
        Transaction(
          microserviceName = name,
          consumerGroup = "writeside",
          nodeIdentification = "1",
          transactionName = "CreateAuction",
          ObjectTransaction(AuctionActor, "CreateAuction", "CreatedAuction"),
          executionContext
        ),
        Transaction(
          microserviceName = name,
          consumerGroup = "writeside",
          nodeIdentification = "1",
          transactionName = "CreatedAuction",
          ObjectTransaction(LotActor, "CreatedAuction", "CreatedAuctionValidated"),
          executionContext
        )
      )
    }
  }

  serve
}
