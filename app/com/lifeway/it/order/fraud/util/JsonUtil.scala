package com.lifeway.it.order.fraud.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.lifeway.it.order.fraud.messaging.ApiError
import com.lifeway.it.order.fraud.event.OrderCreated
import org.json4s.{DefaultFormats, ShortTypeHints, TypeHints}
import org.json4s.native.JsonMethods._
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import com.lifeway.it.order.fraud.event._
import com.lifeway.it.order.fraud.event.paymentSerializer.PaymentCodec
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

object JsonUtil {

  implicit val formats = new DefaultFormats {
    override val typeHintFieldName: String = "eventType"
    override val typeHints: TypeHints = ShortTypeHints(List(classOf[OrderCreated]))

    override val customSerializers = List(PaymentCodec)
  }

  def parseJsonEvent(json: String): Either[ApiError, Event] = {
    Try {
      parse(json).extract[Event]
    } match {
      case Success(result) => Right(result)
      case Failure(error) => Left(ApiError("error", error.getMessage))
    }
  }

  def parseQueuedEvent(json: String): Either[ApiError, QueuedEvent] = {
    Try {
      parse(json).extract[QueuedEvent]
    } match {
      case Success(result) => Right(result)
      case Failure(error) => Left(ApiError("error", error.getMessage))
    }
  }

  def parseJson[T](json: String)(implicit rds: Reads[T]): Either[ApiError, T] = {
    Try {
      Json.parse(json).validate[T].fold(
        failure => Left(ApiError("Json", JsError.toJson(failure).toString())),
        success => Right(success)
      )
    } match {
      case Success(result) => result
      case Failure(error) => Left(ApiError("error", error.getMessage))
    }
  }

  def parseJson[T](json: JsValue)(implicit rds: Reads[T]): Either[ApiError, T] = {
    Try {
      json.validate[T].fold(
        failure => Left(ApiError("Json", JsError.toJson(failure).toString())),
        success => Right(success)
      )
    } match {
      case Success(result) => result
      case Failure(error) => Left(ApiError("error", error.getMessage))
    }
  }

  def toJson(event:Object):String = {
    write(event)
  }

  def now:String = {
    ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
  }
}

