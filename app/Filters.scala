import javax.inject.Inject

import com.lifeway.it.prometheus.PrometheusRequestFilter
import play.api.http.HttpFilters

class Filters @Inject()(prometheusRequestFilter: PrometheusRequestFilter) extends HttpFilters {
  val filters = Seq(prometheusRequestFilter)
}