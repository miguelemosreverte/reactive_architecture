package io.scalac.auction.lot

object Domain {
  case class LotId(id: String)
  case class Lot(id: LotId)

  case class UserId(id: String)
  case class User(id: UserId)

  case class Bid(user: User, lot: Lot, bid: Int)
}
