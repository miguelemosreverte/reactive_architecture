package io.scalac.auction.lot.actor.router

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Routers}
import io.scalac.auction.lot.Protocol.{`Lot Commands`, Command}
import io.scalac.auction.lot.Domain.Lot

import java.util.UUID
import scala.util.{Failure, Success, Try}

object Router {
  type `Lot Router` = ActorRef[`Lot Commands`]

  def apply(lots: Set[Lot])(implicit ctx: ActorContext[_]) = {
    val servUUID = UUID.randomUUID.toString
    val serviceKey = ServiceKey[io.scalac.auction.lot.Protocol.Command](s"sk-$servUUID")

    lots foreach { l =>
      val lotActor = ctx.spawn(io.scalac.auction.lot.actor.Actor.behavior(l), s"lot-$servUUID-${l.id.id}")
      ctx.system.receptionist ! Receptionist.Register(serviceKey, lotActor)
    }

    val group = Routers
      .group(serviceKey)
      .withConsistentHashingRouting(1, _.id.id)
    val router: `Lot Router` = ctx.spawn(group, s"auction-$servUUID")
    router
  }
}
