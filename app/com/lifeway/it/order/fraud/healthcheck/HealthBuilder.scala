package com.lifeway.it.order.fraud.healthcheck

import com.lifeway.it.order.fraud.healthcheck.HealthBuilder.{AppHealth, Check, IntegrationHealth}
import play.api.libs.json.{Json, Writes}

import scala.collection.mutable.ArrayBuffer

class HealthBuilder() {

  private val appName: String = "Fraud Service" //BuildInfo.name
  private var checks: ArrayBuffer[Check] = new ArrayBuffer()
  private var healthy = true

  def addCheck(item: String, ok: Boolean): HealthBuilder = {
    checks += Check(item, ok)
    healthy = healthy && ok
    this
  }

  def buildApp: AppHealth = {
    AppHealth(appName, checks, healthy)
  }

  def buildIntegration: IntegrationHealth = {
    IntegrationHealth(appName, checks, healthy)
  }

}

object HealthBuilder {

  case class Check(item: String, ok: Boolean)

  case class AppHealth(appName: String, appChecks: Seq[Check], appHealthy: Boolean)

  case class IntegrationHealth(appName: String, integrationChecks: Seq[Check], integrationHealthy: Boolean)

  implicit val checkWrites: Writes[Check] = Json.writes[Check]
  implicit val appHealthWrites: Writes[AppHealth] = Json.writes[AppHealth]
  implicit val integrationHealthWrites: Writes[IntegrationHealth] = Json.writes[IntegrationHealth]

}
