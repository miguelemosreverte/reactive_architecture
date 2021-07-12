package io.scalac.auction.auction.actor.states

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.lot.actor.router.Router
import io.scalac.auction.lot.actor.router.Router.`Lot Router`
import io.scalac.auction.auction.Domain.Auction
import io.scalac.auction.auction.Protocol.{Command, Commands}
import io.scalac.auction.auction.Protocol.Responses.{Failures, Successes}

object InProgress {
  def apply(
      auction: Auction
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, message) =>
      implicit val lots: `Lot Router` = Router(auction.lots)(ctx)
      message match {

        case Commands.Start(id, replyTo) =>
          replyTo ! Failures.`cannot start auction again`
          Behaviors.same

        case Commands.End(id, replyTo) =>
          replyTo ! Successes.Ended(id)
          Finished(auction)

        case Commands.`bid lot`(auctionId, bid, replyTo) =>
          lots ! io.scalac.auction.lot.Protocol.Commands.Bid(bid, replyTo)
          Behaviors.same

      }
    }
}
