package io.scalac.auction.lot.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.lot.Domain.{Bid, Lot}
import io.scalac.auction.lot.Protocol.Command
import io.scalac.auction.lot.{Domain, Logic, Protocol}

object Actor {

  def behavior(
      lot: Lot,
      bids: Set[Bid] = Set.empty,
      highestBid: Option[Int] = None
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case c @ Protocol.Commands.Bid(b @ Domain.Bid(user, lot, bid), replyTo) =>
        Logic(bids, highestBid)(c) match {
          case Left(value) =>
            replyTo ! value
            Behaviors.same
          case Right(value) =>
            replyTo ! value
            behavior(lot, bids + b, Some(0))

        }

    }

}
