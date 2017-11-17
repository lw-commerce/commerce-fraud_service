package com.lifeway.it.order.fraud.event

import java.util.UUID

import com.lifeway.it.order.fraud.util.JsonUtil

abstract class FraudEvent(orderNumber:String) extends Event {
  def id:String = UUID.randomUUID().toString
  def timestamp:String = JsonUtil.now
}

case class FraudCheckPassed(orderNumber:String) extends FraudEvent(orderNumber)

case class FraudCheckFailed(orderNumber:String, failedRules:List[String]) extends FraudEvent(orderNumber)
