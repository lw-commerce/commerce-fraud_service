package com.lifeway.it.order.fraud.event

import java.util.UUID

case class PublicEvent (id:String = UUID.randomUUID().toString, eventType:String, domain:String = "lwFraud")
