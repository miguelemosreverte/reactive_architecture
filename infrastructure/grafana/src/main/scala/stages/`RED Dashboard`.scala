package stages

import stages.add_dashboards.Implicits._

case class `RED Dashboard`(preffix: String, name: String) {
      val asJson = stages.add_dashboards
        .CreateDashboard()
        .overwrite(true)
        .withName(name)
        .addPanel(Panel(
          yaxes = `Panel yAxes`.apply(`Panel yAxes`.Unit),
          title = "request",
          targets = Seq(`Panel Target`(expr = s"${preffix}_counter_total{entity=\"${name}-request\"}"))
        ))
        .addPanel(Panel(
          yaxes = `Panel yAxes`.apply(`Panel yAxes`.Unit),
          title = "error",
          targets = Seq(`Panel Target`(expr = s"${preffix}_counter_total{entity=\"${name}-error\"}"))
        ))
        .addPanel(Panel(
          yaxes = `Panel yAxes`.apply(`Panel yAxes`.Time),
          title = "duration",
          targets = Seq(`Panel Target`(expr = s"${preffix}_histograms_bucket{le=\"300.0\",entity=\"${name}-duration\"}"))
        ))
        .prepare
        .asJson
    }