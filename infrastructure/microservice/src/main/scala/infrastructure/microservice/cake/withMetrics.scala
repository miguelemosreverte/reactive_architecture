package infrastructure.microservice.cake

import com.typesafe.config.ConfigFactory
import infrastructure.microservice.Microservice
import kamon.Kamon
import monitoring.Monitoring
import monitoring.interpreter.kamon.KamonMonitoring

trait withMetrics {
  self: Microservice =>
  Kamon.init(
    ConfigFactory.parseString("""
        |kamon.metric.tick-interval = 5 seconds""".stripMargin).withFallback(ConfigFactory.load())
  )
  implicit val metrics: Monitoring = new KamonMonitoring(name)
}
