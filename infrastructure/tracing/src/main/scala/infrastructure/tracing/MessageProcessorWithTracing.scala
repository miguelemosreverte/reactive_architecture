package infrastructure.tracing

import infrastructure.kafka.algebra.MessageProcessor

trait MessageProcessorWithTracing[Message <: Tracing] extends MessageProcessor[Message]
