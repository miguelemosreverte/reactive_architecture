package io.scalac.auction.auction

import akka.actor.typed.ActorRef
import io.scalac.auction.auction.Domain.{Auction, AuctionId}
import io.scalac.auction.lot.Domain.{Bid, Lot, LotId}

sealed trait Protocol
object Protocol {

  type `Auction Commands` = io.scalac.auction.auction.Protocol.Command

  sealed trait Response extends Protocol
  sealed trait Command extends Protocol {
    val id: AuctionId
  }
  sealed trait Query[Response] extends Protocol {
    def replyTo: ActorRef[Response]
  }

  object Commands {
    case class Start(id: AuctionId, replyTo: ActorRef[Response]) extends Command
    case class End(id: AuctionId, replyTo: ActorRef[Response]) extends Command

    sealed trait `Lot related commands` extends Command
    case class `add lot`(
        id: AuctionId,
        lot: Lot,
        replyTo: ActorRef[Response]
    ) extends `Lot related commands`
    case class `remove lot`(
        id: AuctionId,
        lot: Lot,
        replyTo: ActorRef[Response]
    ) extends `Lot related commands`
    case class `bid lot`(
        id: AuctionId,
        bid: Bid,
        replyTo: ActorRef[io.scalac.auction.lot.Protocol.Response]
    ) extends `Lot related commands`
  }

  object Queries {
    case class `get all auctions`(replyTo: ActorRef[Response]) extends Query[Response]
    case class `get lots by auction`(auctionId: AuctionId, replyTo: ActorRef[Response]) extends Query[Response]
  }

  object Responses {
    case class `lots by action`(auctionId: AuctionId, lots: Set[Lot]) extends Response
    case class `all auctions`(auctions: Set[Auction]) extends Response

    sealed trait Failure extends Response
    object Failures {
      case object `cannot start empty auction` extends Failure
      case object `cannot end empty auction` extends Failure
      case object `cannot start auction again` extends Failure
      case object `cannot restart auction` extends Failure
      case object `cannot end auction again` extends Failure
      case object `cannot end auction that has not started` extends Failure
    }
    sealed trait Success extends Response
    object Successes {
      case class Started(auction: AuctionId) extends Success
      case class Ended(auction: AuctionId) extends Success

      sealed trait `Lot related responses` extends Success
      case object `added lot` extends `Lot related responses`
      case object `removed lot` extends `Lot related responses`
    }
  }

}
