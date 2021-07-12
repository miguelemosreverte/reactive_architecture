package io.scalac.auction.lot

import akka.actor.typed.ActorRef
import io.scalac.auction.lot.Domain.LotId

sealed trait Protocol

object Protocol {

  type `Lot Commands` = io.scalac.auction.lot.Protocol.Command

  sealed trait Response extends Protocol
  sealed trait Command extends Protocol {
    def id: LotId
  }

  object Commands {
    case class Bid(bid: Domain.Bid, replyTo: ActorRef[Response]) extends Command {
      val id = bid.lot.id
    }
  }

  object Responses {
    sealed trait Success extends Response
    object Successes {
      case class BidSuccessful(highest: Int, winning: Boolean) extends Success
    }
    sealed trait Failure extends Response
    object Failures {
      case object `bid must be positive` extends Failure
    }
  }

}
