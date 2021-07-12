package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

case class LotId(id: String)

case object LotId extends `JSON Serialization`[LotId] with Example[LotId] {
  override val json: Format[LotId] = Json.format
  val example = LotId("example lot")
}
