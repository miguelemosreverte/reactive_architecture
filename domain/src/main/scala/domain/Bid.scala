package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

case class Bid(user: User, lot: Lot, bid: Int)

case object Bid extends `JSON Serialization`[Bid] with Example[Bid] {
  override val json: Format[Bid] = Json.format

  override val example = Bid(User.example, Lot.example, bid = 10)
}
