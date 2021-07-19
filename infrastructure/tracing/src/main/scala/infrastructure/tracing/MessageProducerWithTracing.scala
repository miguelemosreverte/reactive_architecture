package infrastructure.tracing

import infrastructure.kafka.algebra.MessageProducer

trait MessageProducerWithTracing[Message <: Tracing] extends MessageProducer[Message]
