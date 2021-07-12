package adaptors

import infrastructure.kafka.http.HttpToKafka.TopicToWrite

object User {
  def apply(): TopicToWrite = TopicToWrite(
    endpointName = "user",
    topic = "user",
    deserializationValidator = domain.User,
    example = domain.User.serialize(domain.User.example, multiline = false)
  )
}
