package io.scalac.auction.auction.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, Routers}
import io.scalac.auction.auction.Domain.Auction
import io.scalac.auction.auction.actor.states.{Closed, InProgress}

object Actor {

  def apply(auction: Auction) =
    Closed(auction)

}
