package io.scalac.auction.auction.actor.states

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import io.scalac.auction.auction.Domain.Auction
import io.scalac.auction.auction.Protocol.{Command, Commands}
import io.scalac.auction.auction.Protocol.Responses.{Failures, Successes}

object InProgress {
  def apply(
      auction: Auction
  )(
      implicit lotActor: ActorRef[ShardingEnvelope[io.scalac.auction.lot.Protocol.Command]]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, message) =>
      message match {

        case Commands.Start(id, replyTo) =>
          replyTo ! Failures.`cannot start auction again`
          Behaviors.same

        case Commands.End(id, replyTo) =>
          replyTo ! Successes.Ended(id)
          Finished(auction)

        case Commands.`bid lot`(auctionId, bid, replyTo) =>
          lotActor ! ShardingEnvelope(bid.lot.id.id, io.scalac.auction.lot.Protocol.Commands.Bid(bid, replyTo))
          Behaviors.same

      }
    }
}
