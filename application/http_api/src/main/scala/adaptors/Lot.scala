package adaptors
import infrastructure.kafka.http.HttpToKafka.TopicToWrite

object Lot {
  def apply(): TopicToWrite = TopicToWrite(
    endpointName = "lot",
    topic = "lot",
    deserializationValidator = domain.Lot,
    example = domain.Lot.serialize(domain.Lot.example, multiline = false)
  )
}
