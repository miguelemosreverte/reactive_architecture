package io.scalac.auction.auction

import io.scalac.auction.lot.Domain.Lot

object Domain {
  case class AuctionId(id: String)

  case class Auction(id: AuctionId, lots: Set[Lot])
}
