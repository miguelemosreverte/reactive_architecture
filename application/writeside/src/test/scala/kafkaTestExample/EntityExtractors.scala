package kafkaTestExample

import kafkaTestExample.Domain._

object EntityExtractors {
  implicit def `CreateAuction domainEntityIdExtractor`: CreateAuction => String =
    _.id

  implicit def `CreatedAuction domainEntityIdExtractor`: CreatedAuction => String =
    _.id

  implicit def `CreatedAuctionValidated domainEntityIdExtractor`: CreatedAuctionValidated => String =
    _.id
}
