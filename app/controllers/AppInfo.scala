package controllers

import javax.inject.Inject

import com.lifeway.it.order.fraud.build.BuildInfo
import com.lifeway.it.order.fraud.healthcheck.HealthBuilder
import com.lifeway.it.order.fraud.healthcheck.HealthBuilder.{AppHealth, IntegrationHealth}
import com.lifeway.it.order.fraud.service.HealthCheckService
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

class AppInfo @Inject()(healthCheckService: HealthCheckService, components: ControllerComponents) extends AbstractController(components) {

  implicit val classLoader = getClass.getClassLoader

  def version = Action {
    Ok(Json.parse(BuildInfo.toJson))
  }

  def healthcheck = Action {
    Ok(Json.toJson[AppHealth](getAppHealth))
  }

  def appHealth = Action {
    Ok(Json.toJson[AppHealth](getAppHealth))
  }

  def integrationHealth = Action {
    Ok(Json.toJson[IntegrationHealth](getIntegrationHealth))
  }

  def getAppHealth: AppHealth = {
    new HealthBuilder()
        .buildApp
  }

  def getIntegrationHealth(): IntegrationHealth = {
    new HealthBuilder()
        .buildIntegration
  }
}
