package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

case class WinningBid(bid: Bid)

case object WinningBid extends `JSON Serialization`[WinningBid] with Example[WinningBid] {
  override val json: Format[WinningBid] = Json.format
  override val example = WinningBid(Bid.example)
}
