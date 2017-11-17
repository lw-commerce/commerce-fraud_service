package com.lifeway.it.order.fraud

import com.lifeway.it.order.fraud.actor.FraudActorSystem
import play.api.ApplicationLoader
import play.api.ApplicationLoader._
import com.lifeway.it.order.fraud.service.DefaultHealthCheckService
import com.lifeway.it.prometheus.{PrometheusController, PrometheusRequestFilter}
import play.api.BuiltInComponentsFromContext
import router._
import controllers._
import com.lifeway.it.order.fraud.messaging._
import com.lifeway.it.order.fraud.build.BuildInfo


class FraudApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new FraudApplicationComponents(context).application
  }
}


object ApplicationComponents {
  val DEPLOYID: String = BuildInfo.buildTime.toString
}


class FraudApplicationComponents(context: Context) extends BuiltInComponentsFromContext(context) {

  // routes
  override lazy val router = new Routes(
    httpErrorHandler,
    appInfoController,
    prometheusController
  )

  val prometheusMetrics = new PrometheusRequestFilter()
  override lazy val httpFilters = Seq(prometheusMetrics)

  lazy val mongoDBService = new DefaultMongoDBService(AppConfiguration.mongoUri, AppConfiguration.db, AppConfiguration.sslInvalidHostNameAllowed)
  lazy val kafkaStreamConsumerRepo = new DefaultKafkaStreamStateRepository(mongoDBService)
  lazy val queuedEventRepository = new DefaultQueuedEventRepository(mongoDBService)


  //Controllers
  lazy val appInfoController = new AppInfo(healthCheckService, controllerComponents)
  lazy val prometheusController = new PrometheusController(controllerComponents)

  // Services
  lazy val healthCheckService = new DefaultHealthCheckService(context.initialConfiguration)

  FraudActorSystem.start(kafkaStreamConsumerRepo, queuedEventRepository)
}
