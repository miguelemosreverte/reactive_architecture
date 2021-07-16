package infrastructure.kafka.algebra

trait KafkaTransaction {
  def from: String
  def to: String
}
