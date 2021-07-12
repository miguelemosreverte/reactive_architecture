package io.scalac.auction.auction.actor.states

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.scalac.auction.lot.actor.router.Router.`Lot Router`
import io.scalac.auction.auction.Domain.Auction
import io.scalac.auction.auction.Protocol.{Command, Commands}
import io.scalac.auction.auction.Protocol.Responses.Failures

object Finished {

  def apply(
      auction: Auction
  )(implicit lots: `Lot Router`): Behavior[Command] =
    Behaviors.receiveMessage {

      case Commands.Start(id, replyTo) =>
        replyTo ! Failures.`cannot restart auction`
        Behaviors.same

      case Commands.End(id, replyTo) =>
        replyTo ! Failures.`cannot end auction again`
        Behaviors.same

    }
}
