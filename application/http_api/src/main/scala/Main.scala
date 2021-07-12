import adaptors.Adaptors
import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import infrastructure.kafka.http.HttpToKafka
import infrastructure.kafka.http.HttpToKafka.{KafkaEndpoint, TopicToWrite}
import akka.http.scaladsl.server.Directives._

object Main extends App {

  case class Config(
      kafkaBootstrapServers: String = "0.0.0.0:29092",
      `whitelisted topics`: Option[Set[String]] = None,
      `blacklisted topics`: Option[Set[String]] = None,
      group: String = "HTTP_to_Kafka",
      host: String = "0.0.0.0",
      port: Int = 8081
  )

  object Menu {

    def apply(args: Array[String]): Option[Config] = {
      import scopt.OParser
      val builder = OParser.builder[Config]
      val parser1 = {
        import builder._
        OParser.sequence(
          programName("Kafka Topic as Websocket"),
          head("Kafka topics published to websocket", "1.0.0"),
          // option --kafka
          opt[String]("kafka")
            .action((x, c) => c.copy(kafkaBootstrapServers = x))
            .text("URL of Kafka: By default: 0.0.0.0:29092"),
          // option --host
          opt[String]("host")
            .action((x, c) => c.copy(host = x))
            .text("Host of the websocket"),
          // option --port
          opt[Int]("port")
            .action((x, c) => c.copy(port = x))
            .text("Port of the websocket"),
          // option --whitelist
          opt[Seq[String]]("whitelist")
            .valueName("whitelist")
            .action((x, c) =>
              if (x.isEmpty) c.copy(`whitelisted topics` = None)
              else c.copy(`whitelisted topics` = Some(x.toSet))
            )
            .text("Explicitely define what Kafka topics are available for exposure to write from HTTP."),
          // option --blacklist
          opt[Seq[String]]("blacklist")
            .valueName("blacklist")
            .action((x, c) =>
              if (x.isEmpty) c.copy(`blacklisted topics` = None)
              else c.copy(`blacklisted topics` = Some(x.toSet))
            )
            .text("Explicitely define what Kafka topics are dissallowed from exposure to write from HTTP"),
          // option --group
          opt[String]("group")
            .action((x, c) => c.copy(group = x))
            .text("Kafka consumer group"),
          help("help").text(s"""
                              |
                              | You can use tool to expose the stream of messages from 
                              | Kafka topics to a single Websocket server.
                              | 
                              | By default it will expose the following topics to be populated via HTTP POST:
                              | 
                              | ${Adaptors().map(_.topic).map(t => s"\t-$t").mkString("\n")}
                              |
                              |""".stripMargin)
        )
      }

      // OParser.parse returns Option[Config]
      OParser.parse(parser1, args, Config())
    }
  }

  Menu(args) match {
    case Some(config) =>
      println(config)
      implicit def actorSystem: ActorSystem =
        ActorSystem.create("HTTP_to_Kafka", system_config.withFallback(ConfigFactory.load()))
      // https://doc.akka.io/docs/akka-http/current/handling-blocking-operations-in-akka-http-routes.html
      def system_config = ConfigFactory.parseString("""
       |my-blocking-dispatcher {
       |  type = Dispatcher
       |  executor = "thread-pool-executor"
       |  thread-pool-executor {
       |    fixed-pool-size = 16
       |  }
       |  throughput = 1
       |}
       |""".stripMargin)
      implicit val blockingDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup("my-blocking-dispatcher")
      implicit val kafkaEndpoint: KafkaEndpoint = KafkaEndpoint(config.kafkaBootstrapServers)

      val adaptors = Adaptors()
        .filter(`from HTTP POST to Kafka` =>
          config.`whitelisted topics` match {
            case Some(allowed) => allowed contains `from HTTP POST to Kafka`.topic
            case None => true
          }
        )
        .filterNot(`from HTTP POST to Kafka` =>
          config.`blacklisted topics` match {
            case Some(not_allowed) => not_allowed contains `from HTTP POST to Kafka`.topic
            case None => false
          }
        )

      println(
        s"""
          |Exposing the following topics for HTTP POST:
          |
          | ${adaptors.map(_.topic).map(t => s"\t-$t").mkString("\n")}
          |
          |To hit the API you can try the following commands:
          | ${adaptors
             .map(t => s""" 
                  |[${t.endpointName}]
                  |curl -d '${t.example}' -H "Content-Type: application/json" -X POST http:""" + "//" + s"""${config.host}:${config.port}/${t.endpointName} 
                  |""".stripMargin)
             .mkString("\n")}
          | 
          |
          |""".stripMargin
      )
      val route: Route =
        adaptors
          .map(HttpToKafka(_))
          .reduce(_ ~ _)

      val bindingFuture = Http().bindAndHandle(route, config.host, config.port)

      System.out.println(s"""
                            |Server online at http://${config.host}:${config.port}
                            |Press RETURN to stop...
                            |""".stripMargin)
      System.in.read // let it run until user presses return

      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => actorSystem.terminate()) // and shutdown when done

    case _ =>
    // arguments are bad, error message will have been displayed
  }

}
