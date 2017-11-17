package com.lifeway.it.order.fraud.service

import com.lifeway.it.order.fraud.event.{FraudCheckFailed, FraudCheckPassed, FraudEvent, Order}

object FraudCheckService {
  def checkForFraud(order:Order): FraudEvent = {
    RuleSet.rules.filter(_.fail(order)).map(_.description) match {
      case Nil => FraudCheckPassed(order.orderNumber)
      case failedRules:List[String] => FraudCheckFailed(order.orderNumber, failedRules)
    }
  }
}

trait Rule {
  def description:String
  def fail(order:Order):Boolean
}

object DifferentAddressesRule extends Rule {
  override def description = "Shipping and Billing address do not match and over $125"

  override def fail(order:Order) = {
    order.orderTotal > 125.00 && (order.billingAddress.isDefined && order.shippingAddress.isDefined && order.shippingAddress != order.billingAddress)
  }
}

object InternationalRule extends Rule {
  override def description = "International Order over $30"

  override def fail(order:Order) = {
    order.orderTotal > 30.00 && order.shippingAddress.isDefined && order.shippingAddress.get.country != "US"
  }
}

object AmexRule extends Rule {
  override def description = "AMEX and over $50"

  override def fail(order:Order) = {
    order.creditCardType.get == "AMEX" && order.orderTotal > 50.00
  }
}

object OrderTotalRule extends Rule {
  override def description = "Order greater than $400"

  override def fail(order:Order) = {
    order.orderTotal > 400.00
  }
}


object RuleSet {
  val rules = DifferentAddressesRule :: InternationalRule :: AmexRule :: OrderTotalRule :: Nil
}
