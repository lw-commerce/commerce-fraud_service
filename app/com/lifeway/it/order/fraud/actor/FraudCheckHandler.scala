package com.lifeway.it.order.fraud.actor

import akka.actor.{Actor, ActorLogging}
import com.lifeway.it.order.fraud.event.OrderCreated
import com.lifeway.it.order.fraud.service.FraudCheckService

class FraudCheckHandler extends Actor with ActorLogging {
  override def receive = {
    case event: OrderCreated => handle(event)
    case _: Any => //ignore
  }

  def handle(orderCreated:OrderCreated): Unit = {
    log.debug("FraudCheckHandler received event: " + orderCreated)

    val result = FraudCheckService.checkForFraud(orderCreated.payload)

    FraudActorSystem.eventPublisher ! result
  }
}
