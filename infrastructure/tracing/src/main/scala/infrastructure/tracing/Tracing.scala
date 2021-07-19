package infrastructure.tracing

trait Tracing {
  val fromId: String
  val id: String
  val timestamp: Tracing.Timestamp
}

object Tracing {

  case class Timestamp(
      arrival: Long,
      processed: Long
  )
}
