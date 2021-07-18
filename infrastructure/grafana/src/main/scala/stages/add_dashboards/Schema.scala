package stages.add_dashboards
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.Json
import stages.add_dashboards.Implicits.{`Panel GridPosition`, `Panel xAxis`, `Panel`}

import java.util.UUID

case class CreateDashboard(
    folderId: Int = 0,
    overwrite: Boolean = true,
    dashboard: Implicits.`Dashboard` = Implicits.`Dashboard`.example
) {

  def overwrite(yesNo: Boolean) =
    copy(overwrite = yesNo)

  def withName(name: String) =
    copy(
      dashboard = dashboard.copy(
        title = name
      )
    )

  def withId(id: Int) =
    copy(
      dashboard = dashboard.copy(
        id = Some(id)
      )
    )
  def addPanel(panel: `Panel`) = copy(
    dashboard = dashboard.copy(
      panels = dashboard.panels :+ panel
    )
  )
  def prepare: CreateDashboard =
    copy(
      dashboard = dashboard.copy(
        panels = dashboard.panels.zipWithIndex.map {
          case (panel, index) =>
            panel.copy(
              id = index,
              gridPos = `Panel GridPosition`(
                h = 7, //left is 7 7 7
                w = 7, // left is 7 8 9
                x = (index % 3) match {
                  case 0 => 0
                  case 1 => 7
                  case 2 => 14
                }

                /*index match {
                  case 0 => 0
                  case 1 => 7
                  case 2 => 17
                }*/, // left is 0 7 15
                y = index / 3 // left is 0 0 0
              )
            )
        }
      )
    )
}

object Implicits {
  implicit object CreateDashboard extends `JSON Serialization`[CreateDashboard] {
    val example = stages.add_dashboards.CreateDashboard()
    override val json = Json.format
  }

  case class `Dashboard Time`(from: String = "now-6h", to: String = "now")
  implicit object `Dashboard Time` extends `JSON Serialization`[`Dashboard Time`] {
    val example = `Dashboard Time`.apply()
    override val json = Json.format
  }
  case class `Dashboard`(
      id: Option[Int] = None,
      editable: Boolean = true,
      timezone: String = "browser",
      title: String = "Production Overview 2 -- light -- from Insomnia",
      uid: String = UUID.randomUUID().toString,
      version: Int = 1,
      panels: Seq[Panel] = Seq.empty,
      schemaVersion: Int = 21,
      style: String = "light",
      tags: Seq[String] = Seq("templated"),
      time: `Dashboard Time` = `Dashboard Time`.example
  )
  implicit object `Dashboard` extends `JSON Serialization`[`Dashboard`] {
    val example = `Dashboard`.apply()
    override val json = Json.format
  }

  case class `Panel GridPosition`(
      h: Int = 7,
      w: Int = 7,
      x: Int = 0,
      y: Int = 1
  )
  implicit object `Panel GridPosition` extends `JSON Serialization`[`Panel GridPosition`] {
    val example = `Panel GridPosition`()
    override val json = Json.format
  }
  case class `Panel Target`(
      expr: String = "example_counter_total{entity=\"counter\"}",
      refId: String = "A"
  )
  implicit object `Panel Target` extends `JSON Serialization`[`Panel Target`] {
    val example = `Panel Target`()
    override val json = Json.format
  }
  case class `Panel xAxis`(mode: String = "time")
  implicit object `Panel xAxis` extends `JSON Serialization`[`Panel xAxis`] {
    val example = `Panel xAxis`()
    override val json = Json.format
  }
  case class `Panel yAxes`(format: String = "short")
  implicit object `Panel yAxes` extends `JSON Serialization`[`Panel yAxes`] {
    val example = `Panel yAxes`()
    override val json = Json.format
  }
  case class `Panel yAxis`(align: Boolean = false)
  implicit object `Panel yAxis` extends `JSON Serialization`[`Panel yAxis`] {
    val example = `Panel yAxis`()
    override val json = Json.format
  }
  case class `Panel`(
      bars: Boolean = false,
      dashLength: Int = 10,
      dashes: Boolean = false,
      datasource: String = "prometheus",
      gridPos: `Panel GridPosition` = `Panel GridPosition`.example,
      id: Int = 0,
      lines: Boolean = true,
      lineswidth: Int = 1,
      targets: Seq[`Panel Target`] = Seq(`Panel Target`.example),
      title: String = "Panel Title Left",
      `type`: String = "graph",
      xAxis: `Panel xAxis` = `Panel xAxis`.example,
      yAxes: Seq[`Panel yAxes`] = Seq(`Panel yAxes`.example, `Panel yAxes`.example),
      yAxis: `Panel yAxis` = `Panel yAxis`.example
  )

  implicit object `Panel` extends `JSON Serialization`[`Panel`] {
    val example = `Panel`.apply()
    override val json = Json.format
  }

  case class CreateDashboardResponse(
      id: Int,
      slug: String,
      status: String,
      uid: String,
      url: String,
      version: Int
  )

  implicit object CreateDashboardResponse extends `JSON Serialization`[CreateDashboardResponse] {
    val example = CreateDashboardResponse(
      id = 1,
      slug = "production-overview-2-light-from-insomnia",
      status = "success",
      uid = "j6-5x3ink",
      url = "/d/j6-5x3ink/production-overview-2-light-from-insomnia",
      version = 30
    )
    override val json = Json.format
  }

}
