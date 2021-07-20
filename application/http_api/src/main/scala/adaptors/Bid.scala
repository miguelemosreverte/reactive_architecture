package adaptors
import infrastructure.kafka.http.HttpToKafka.TopicToWrite

object Bid {
  def apply(): TopicToWrite = TopicToWrite(
    endpointName = "bid",
    topic = "bid",
    deserializationValidator = domain.Bid,
    example = domain.Bid.serialize(domain.Bid.example, multiline = false)
  )
}
