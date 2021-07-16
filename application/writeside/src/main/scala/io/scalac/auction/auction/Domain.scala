package io.scalac.auction.auction

import io.scalac.auction.lot.Domain.Lot

object Domain {
  case class AuctionId(id: String)

  case class Auction(lots: Set[Lot])
  object Auction {
    def empty = Auction(Set.empty)
  }
}
