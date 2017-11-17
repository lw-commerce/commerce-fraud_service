package com.lifeway.it.order.fraud.event

case class QueuedEvent(_id: String,
                       topic: String,
                       seq: Long,
                       event: FraudEvent,
                       key: Option[String] = None)