package stages.set_api_key

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

object Domain {

  case class CreateApiKey(
      name: String,
      role: String
  )

  implicit object CreateApiKey extends `JSON Serialization`[CreateApiKey] {
    val example = CreateApiKey(name = "apikeycurl", role = "Admin")
    val json: Format[CreateApiKey] = Json.format
  }

  case class GetApiKeyResponse(
      name: String = "apikeycurl",
      key: String = "eyJrIjoiOFdwcU03aHRjREhlN0swaVhTTDE5NHVGWHZOc2xUQWgiLCJuIjoiYXBpa2V5Y3VybCIsImlkIjoxfQ=="
  )

  implicit object GetApiKeyResponse extends `JSON Serialization`[GetApiKeyResponse] {
    val example = GetApiKeyResponse()
    val json: Format[GetApiKeyResponse] = Json.format
  }

}
