package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

case class Lot(id: LotId)

case object Lot extends `JSON Serialization`[Lot] with Example[Lot] {
  override val json: Format[Lot] = Json.format
  val example = Lot(LotId.example)
}
