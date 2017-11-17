package com.lifeway.it.order.fraud

import com.google.inject.AbstractModule
import com.lifeway.it.order.fraud.service._

class GuiceInjectionModule() extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[HealthCheckService]).to(classOf[DefaultHealthCheckService])
  }
}
