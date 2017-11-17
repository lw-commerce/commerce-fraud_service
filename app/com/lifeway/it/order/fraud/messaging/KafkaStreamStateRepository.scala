package com.lifeway.it.order.fraud.messaging

import com.lifeway.it.order.fraud.messaging.KafkaStreamConsumer.KafkaStreamState
import com.lifeway.it.order.fraud.util.JsonUtil
import com.mongodb._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.util.JSON
import play.api.libs.json.Json

import scala.util.control.Exception.catching

trait KafkaStreamStateRepository {
  def save(state: KafkaStreamState): Either[ApiError, Boolean]

  def find(id: String): Either[ApiError, Option[KafkaStreamState]]

  def delete(id: String): Either[ApiError, Int]
}

class DefaultKafkaStreamStateRepository(mongoDBService: MongoDBService) extends KafkaStreamStateRepository {
  val collection = mongoDBService.getCollection("kafkaStreamState")

  val errorCatch = catching(
    classOf[UnsupportedOperationException],
    classOf[WriteConcernException],
    classOf[MongoException],
    classOf[DuplicateKeyException])
      .withApply(e => Left(ApiError("ConsumedEventsRepositoryError", e.getMessage)))

  override def save(state: KafkaStreamState): Either[ApiError, Boolean] = {
    errorCatch {
      collection.save(JSON.parse(Json.stringify(Json.toJson(state))).asInstanceOf[DBObject], WriteConcern.MAJORITY)
      Right(true)
    }
  }

  override def find(id: String): Either[ApiError, Option[KafkaStreamState]] = {
    errorCatch {
      collection.findOne(MongoDBObject("_id" -> id))
        .map { result =>
          JsonUtil.parseJson[KafkaStreamState](result.toString) match {
            case Left(error) => Left(ApiError("ConsumedEventsRepositoryError", error.message))
            case Right(cart) => Right(Some(cart))
          }
        } getOrElse Right(None)
    }
  }

  override def delete(id: String): Either[ApiError, Int] = {
    errorCatch {
      Right(collection.remove(MongoDBObject("_id" -> id)).getN)
    }
  }
}

