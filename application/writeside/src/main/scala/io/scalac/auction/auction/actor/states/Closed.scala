package io.scalac.auction.auction.actor.states

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.cluster.sharding.typed.ShardingEnvelope
import io.scalac.auction.auction.Domain.Auction
import io.scalac.auction.auction.Protocol.Responses.Successes.{`added lot`, `removed lot`}
import io.scalac.auction.auction.Protocol.{Command, Commands}
import io.scalac.auction.auction.Protocol.Responses.{Failures, Successes}
import io.scalac.auction.auction.actor.Actor

object Closed {
  def apply(id: String)(auction: Auction = Auction.empty)(
      implicit lotActor: ActorRef[ShardingEnvelope[io.scalac.auction.lot.Protocol.Command]]
  ): Behavior[Command] =
    Behaviors.receiveMessage {

      case Commands.`add lot`(auctionId, lot, replyTo) =>
        replyTo ! `added lot`
        Closed(id)(auction.copy(lots = auction.lots + lot))

      case Commands.`remove lot`(auctionId, lot, replyTo) =>
        replyTo ! `removed lot`
        Closed(id)(auction.copy(lots = auction.lots - lot))

      case Commands.Start(id, replyTo) =>
        replyTo ! Successes.Started(id)
        InProgress(auction)

      case Commands.End(id, replyTo) =>
        replyTo ! Failures.`cannot end auction that has not started`
        Behaviors.same

    }
}
