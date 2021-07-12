package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

case class User(id: UserId)

case object User extends `JSON Serialization`[User] with Example[User] {
  override val json: Format[User] = Json.format
  val example = User(UserId.example)
}
