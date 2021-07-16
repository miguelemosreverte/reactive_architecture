package io.scalac.auction.lot

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

object Domain {
  case class LotId(id: String)
  object LotId extends `JSON Serialization`[LotId] {
    val example = LotId("1")
    val json: Format[LotId] = Json.format
  }
  case class Lot(id: LotId)
  object Lot extends `JSON Serialization`[Lot] {
    val example = Lot(LotId.example)
    val json: Format[Lot] = Json.format
  }

  case class UserId(id: String)
  object UserId extends `JSON Serialization`[UserId] {
    val example = UserId("1")
    val json: Format[UserId] = Json.format
  }
  case class User(id: UserId)
  object User extends `JSON Serialization`[User] {
    val example = User(UserId.example)
    val json: Format[User] = Json.format
  }

  case class Bid(user: User, lot: Lot, bid: Int)

  object Bid extends `JSON Serialization`[Bid] {
    val example = Bid(User.example, Lot.example, bid = 10)
    val json: Format[Bid] = Json.format
  }
}
