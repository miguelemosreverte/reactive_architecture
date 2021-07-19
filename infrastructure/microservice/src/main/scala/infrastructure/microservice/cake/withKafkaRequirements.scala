package infrastructure.microservice.cake

import com.typesafe.config.ConfigFactory
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.kafka.consumer.logger.Protocol

trait withKafkaRequirements {
  self: withMetrics with withSystem =>
  implicit lazy val kafkaRequirements: KafkaRequirements =
    KafkaRequirements(
      KafkaBootstrapServer(ConfigFactory.load().withFallback(ConfigFactory.parseString("""
          |kafka.url = "0.0.0.0:29092"
          |""".stripMargin)).getString("kafka.url")),
      system,
      logger = {
        case Protocol.`Failed to deserialize`(topic, msg) =>
          monitoring.RED.recordErrors(topic)
        case Protocol.`Failed to process`(topic, msg) =>
          monitoring.RED.recordErrors(topic)
        case Protocol.`Processed`(topic, msg, timeToProcess) =>
          monitoring.RED.recordRequests(topic)
          monitoring.RED.recordDuration(topic, timeToProcess)
      }
    )
}
