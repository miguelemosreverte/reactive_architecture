package infrastructure.kafka.producer.settings

import akka.kafka.{ConsumerSettings, ProducerSettings}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import java.time.Duration
import infrastructure.kafka.KafkaSupport.Protocol._

object Producer {

  def apply(implicit requirements: KafkaRequirements) =
    ProducerSettings
      .create(requirements.actorSystem, new StringSerializer, new StringSerializer)
      .withBootstrapServers(requirements.kafkaBootstrapServer.url)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

}
