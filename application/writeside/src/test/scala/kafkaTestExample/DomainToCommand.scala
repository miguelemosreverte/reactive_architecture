package kafkaTestExample

import akka.actor.typed.ActorRef
import kafkaTestExample.Commands._
import kafkaTestExample.Domain._

object DomainToCommand {

  implicit def `CreateAuction domainToCommand`: (CreateAuction, ActorRef[CreatedAuction]) => CreateAuction_ =
    CreateAuction_.apply

  implicit def `CreatedAuction domainToCommand`
      : (CreatedAuction, ActorRef[CreatedAuctionValidated]) => CreatedAuction_ =
    CreatedAuction_.apply

  implicit def `CreatedAuctionValidated domainToCommand`
      : (CreatedAuctionValidated, ActorRef[Done]) => CreatedAuctionValidated_ =
    CreatedAuctionValidated_.apply
}
