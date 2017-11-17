package com.lifeway.it.order.fraud.service


import akka.util.Timeout
import play.api.{Configuration}

import scala.concurrent.duration._
import scala.language.postfixOps

trait HealthCheckService {
}

class DefaultHealthCheckService (config: Configuration) extends HealthCheckService {
  //simple regex should match one IP address:port, multiple IP address:port, or single localhost:port
  val ipRegex =
  """^((((\d{1,3}\.){3}\d{1,3}:\d{1,5})(,(\d{1,3}\.){3}\d{1,3}:\d{1,5})*)|localhost:\d{1,5})$""".r

  implicit val softCheckTimeout = Timeout(3 seconds)



}


