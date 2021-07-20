package kafkaTestExample

import akka.actor.typed.ActorRef
import kafkaTestExample.Domain._

object Commands {

  case class CreateAuction_(payload: CreateAuction, ref: ActorRef[CreatedAuction])

  case class CreatedAuction_(payload: CreatedAuction, ref: ActorRef[CreatedAuctionValidated])

  case class CreatedAuctionValidated_(payload: CreatedAuctionValidated, ref: ActorRef[Done])
}
