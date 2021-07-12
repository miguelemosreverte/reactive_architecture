package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

case class UserId(id: String)

case object UserId extends `JSON Serialization`[UserId] with Example[UserId] {
  override val json: Format[UserId] = Json.format
  val example = UserId("example user")
}
