import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import infrastructure.http.Client
import infrastructure.http.Client.POST
import stages.add_dashboards._
import stages.add_dashboards.CreateDashboard._
import stages.add_dashboards.Implicits.{CreateDashboard => a, _}

object CreateDashboardMain extends App {
  implicit val actorSystem = ActorSystem("GETAPIKEY")
  implicit val executionContext = actorSystem.dispatcher
  implicit val http = new Client()

  import stages.set_api_key.Domain._

  for {
    done <- http.POST[CreateDashboard, CreateDashboardResponse](
      url = "http://localhost:3000/api/dashboards/db",
      CreateDashboard()
        .overwrite(true)
        .withName("Hello from Scala")
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .addPanel(Panel())
        .prepare,
      headers = Some(
        Authorization(
          OAuth2BearerToken.apply(
            "eyJrIjoiOTRrUUxxOUxGU1laRmlrNHdnakVTdWhvMTJyc3lGTmkiLCJuIjoiaW5zb21uaWEiLCJpZCI6MX0="
          )
        )
      )
    )
  } yield {
    println(done)
  }

}
