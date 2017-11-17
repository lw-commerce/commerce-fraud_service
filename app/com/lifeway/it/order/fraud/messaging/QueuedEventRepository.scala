package com.lifeway.it.order.fraud.messaging

import com.lifeway.it.order.fraud.event.QueuedEvent
import com.lifeway.it.order.fraud.util.JsonUtil
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.util.JSON
import com.mongodb.{DBObject, DuplicateKeyException, MongoException, WriteConcernException}

trait QueuedEventRepository {
  def save(event: QueuedEvent): Either[ApiError, Boolean]

  def retrieveAllQueuedEvents(): Either[ApiError, Option[Seq[QueuedEvent]]]

  def retrieveCurrentSequenceVal(): Either[ApiError, Long]

  def delete(id: String): Either[ApiError, Int]
}

class DefaultQueuedEventRepository(mongoDBService: MongoDBService) extends QueuedEventRepository {

  val collection = mongoDBService.getCollection("queuedEvent")

  import scala.util.control.Exception.catching

  val errorCatch = catching(
    classOf[UnsupportedOperationException],
    classOf[WriteConcernException],
    classOf[MongoException],
    classOf[DuplicateKeyException])
      .withApply(e => Left(ApiError("QueuedEventRepositoryError", e.getMessage)))

  override def save(event: QueuedEvent): Either[ApiError, Boolean] = {
    errorCatch {
      collection.save(JSON.parse(JsonUtil.toJson(event)).asInstanceOf[DBObject])
      Right(true)
    }
  }

  override def retrieveCurrentSequenceVal(): Either[ApiError, Long] = {
    errorCatch {
      collection.findOne(MongoDBObject(), MongoDBObject(), MongoDBObject("seq" -> -1))
          .map { result =>
            JsonUtil.parseQueuedEvent(result.toString) match {
              case Left(error) => Left(ApiError("QueuedEventRepositoryError", error.message))
              case Right(event) => Right(event.seq)
            }
          } getOrElse Right(0)
    }
  }

  override def delete(id: String): Either[ApiError, Int] = {
    errorCatch {
      Right(collection.remove(MongoDBObject("_id" -> id)).getN)
    }
  }

  override def retrieveAllQueuedEvents(): Either[ApiError, Option[Seq[QueuedEvent]]] = {
    errorCatch {
      if (collection.nonEmpty) {
        Right(Some(collection.find().sort(MongoDBObject("seq" -> 1)).toSeq.map { result =>
          JsonUtil.parseQueuedEvent(result.toString) match {
            case Left(error) => None // malformed document, ignore
            case Right(event) => Some(event)
          }
        }
            .filter(x => x.isDefined) // remove all 'none' elements
            .map(x => x.get))) // remove option wrapper

      } else {
        Right(None)
      }
    }
  }
}
