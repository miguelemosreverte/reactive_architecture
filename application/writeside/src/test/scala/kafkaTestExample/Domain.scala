package kafkaTestExample

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

object Domain {

  case class CreateAuction(id: String, lots: Set[String] = Set.empty)
  implicit object CreateAuction extends `JSON Serialization`[CreateAuction] {
    val example = CreateAuction("Old Egypt Treasure", Set.empty)
    val json: Format[CreateAuction] = Json.format
  }

  case class CreatedAuction(id: String, lots: Set[String] = Set.empty)
  implicit object CreatedAuction extends `JSON Serialization`[CreatedAuction] {
    val example = CreatedAuction("Old Egypt Treasure", Set.empty)
    val json: Format[CreatedAuction] = Json.format
  }

  case class CreatedAuctionValidated(id: String, rejected: Boolean, reason: Option[String] = None)
  implicit object CreatedAuctionValidated extends `JSON Serialization`[CreatedAuctionValidated] {
    val example = CreatedAuctionValidated("Old Egypt Treasure", rejected = false, None)
    val json: Format[CreatedAuctionValidated] = Json.format
  }

  case class Done(done: String = "Done")
  implicit object DoneSerializer extends `JSON Serialization`[Done] {
    val example = Done("hi")
    override implicit val json: Format[Done] = Json.format
  }

}
