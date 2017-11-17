package com.lifeway.it.order.fraud.messaging

import play.api.libs.json.Json

case class ApiError(code: String, message: String) extends Exception(message)

object ApiError {
  implicit val apiFormats = Json.format[ApiError]
}
