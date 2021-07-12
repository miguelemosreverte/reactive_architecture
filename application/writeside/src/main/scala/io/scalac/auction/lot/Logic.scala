package io.scalac.auction.lot

import io.scalac.auction.lot.Domain._
import io.scalac.auction.lot.Protocol._
import io.scalac.auction.lot.Protocol.Commands._
import io.scalac.auction.lot.Protocol.Responses.Successes.BidSuccessful
import io.scalac.auction.lot.Protocol.Responses.Failures._

object Logic {

  def calculateHighestBid(bid: Domain.Bid, bids: Set[Domain.Bid]): (Int, Boolean) = {
    val highestBid = (bids + bid).maxBy(bid => bid.bid)
    (highestBid.bid, bid == highestBid)
  }

  type Input = Protocol.Command
  type Output = Either[Protocol.Responses.Failure, Protocol.Responses.Success]

  def apply(
      bids: Set[Domain.Bid],
      highestBid: Option[Int] = None
  )(
      message: Input
  ): Output = {
    message match {
      case Commands.Bid(bid, replyTo) =>
        if (bid.bid <= 0) {
          Left(`bid must be positive`)
        } else {
          val (highest, winning) = calculateHighestBid(bid, bids)
          Right(BidSuccessful(highest, winning))
        }

    }

  }
}
