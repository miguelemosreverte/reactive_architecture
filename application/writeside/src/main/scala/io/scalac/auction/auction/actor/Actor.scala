package io.scalac.auction.auction.actor

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, Routers}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import io.scalac.auction.auction.Domain.{Auction, AuctionId}
import io.scalac.auction.auction.Protocol
import io.scalac.auction.auction.actor.states.{Closed, InProgress}

class Actor(
    implicit sharding: ClusterSharding,
    lot: ActorRef[ShardingEnvelope[io.scalac.auction.lot.Protocol.Command]]
) {

  def apply(id: String)(implicit lot: ActorRef[ShardingEnvelope[io.scalac.auction.lot.Protocol.Command]]) =
    Closed(id)()

  import akka.cluster.sharding.typed.ShardingEnvelope
  import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

  val TypeKey = EntityTypeKey[Protocol.Command]("AuctionActor")

  val shardRegion: ActorRef[ShardingEnvelope[Protocol.Command]] = {
    sharding.init(Entity(TypeKey)(createBehavior = { entityContext =>
      println("RECEIVED AT SHARD REGION")
      apply(entityContext.entityId)
    }))
  }

  val sharded: ActorRef[ShardingEnvelope[Protocol.Command]] = {

    shardRegion
  }

}
