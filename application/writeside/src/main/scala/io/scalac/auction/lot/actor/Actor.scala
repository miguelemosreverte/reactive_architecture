package io.scalac.auction.lot.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import io.scalac.auction.auction.Domain.Auction
import io.scalac.auction.lot.Protocol
import io.scalac.auction.lot.Domain.{Bid, Lot}
import io.scalac.auction.lot.Protocol.Command
import io.scalac.auction.lot.{Domain, Logic, Protocol}

class Actor(
    implicit sharding: ClusterSharding
) {

  def behavior(lotId: String)(
      lot: Option[Lot],
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
            behavior(lotId)(Some(lot), bids + b, Some(0))

        }

    }

  val sharded: ActorRef[ShardingEnvelope[Protocol.Command]] = {

    import akka.cluster.sharding.typed.ShardingEnvelope
    import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

    val TypeKey = EntityTypeKey[Protocol.Command]("LotActor")

    val shardRegion: ActorRef[ShardingEnvelope[Protocol.Command]] = {
      sharding.init(Entity(TypeKey)(createBehavior = entityContext => behavior(entityContext.entityId)(None)))
    }
    shardRegion
  }

}
