package infrastructure.kafka.consumer.settings

import akka.kafka.{ConsumerSettings, ProducerSettings}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import java.time.Duration
import infrastructure.kafka.KafkaSupport.Protocol._

object Consumer {
  def apply(implicit requirements: KafkaRequirements) =
    ConsumerSettings
      .create(requirements.actorSystem, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(requirements.kafkaBootstrapServer.url)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    if (requirements.startFromZero) "earliest" else "auto.offset.reset")
      .withStopTimeout(Duration.ofSeconds(5))

}
