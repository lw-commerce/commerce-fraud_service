package com.lifeway.it.order.fraud.actor

import java.util.UUID

import com.lifeway.it.order.fraud.messaging.ApiError
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.lifeway.it.order.fraud.AppConfiguration
import com.lifeway.it.order.fraud.actor.EventSenderActor.{RetrySendEvents, WriteSuccess}
import com.lifeway.it.order.fraud.event.{FraudEvent, QueuedEvent}
import com.lifeway.it.order.fraud.messaging.{DefaultKafkaProducer, EventProducer, QueuedEventRepository}
import com.lifeway.it.order.fraud.util.JsonUtil
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.language.postfixOps

class EventSenderActorSupervisor(queuedEventRepository: QueuedEventRepository,
                                 retryDelay: FiniteDuration) extends Actor with ActorLogging {

  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._

  import scala.concurrent.duration._

  val domainEventProducer: EventProducer = DefaultKafkaProducer(AppConfiguration.publishTopic, AppConfiguration.kafkaBrokers)

  val eventSenderActor: ActorRef = context.system.actorOf(EventSenderActor.props(
    eventProducer = domainEventProducer,
    queuedEventRepository = queuedEventRepository))

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: Exception =>
        Logger.error(s"********** EventSenderActor died, restarting ${e.getMessage}")
        Restart
    }

  def receive = {
    case e@_ => eventSenderActor.forward(e)
  }
}

class EventSenderActor(eventProducer: EventProducer,
                       queuedEventRepository: QueuedEventRepository,
                       retryDelay: FiniteDuration) extends Actor with ActorLogging {

  import EventSenderActor.{RemoveSuccess, Start}

  var queuedEvents: Seq[QueuedEvent] = Seq.empty[QueuedEvent]

  override def receive: Receive = {
    case Start => log.debug(s"EventSenderActor ${self.path.name} is running")
    case RetrySendEvents => resendQueuedEvents()
    case e: FraudEvent => publish(e)
    case message: Any => log.warning("Unhandled message sent to EventSenderActor: {}", message)
  }

  def whenQueuedEvents: Receive = {
    case RetrySendEvents => resendQueuedEvents()
    case e: FraudEvent => publishQueued(e)
    case (e: FraudEvent, k: String) => publishQueued(e, Some(k))
    case message: Any => log.warning("Unhandled message sent to EventSenderActor: {}", message)
  }

  def nextSequence: Long = if (queuedEvents.isEmpty) 1L else queuedEvents.map(_.seq).max + 1

  def publish(event: FraudEvent, key: String = null): Unit = {
    if (!eventProducer.syncSend(event.toString(), key)) {
      context become whenQueuedEvents

      val jEvent = JsonUtil.toJson(event)

      writeEvent(event, Option(key) ).fold(
        _ => log.error("[CRITICAL] - Could not write event"),
        _ => ()
      )

      scheduleRetry()
    }
  }

  def publishQueued(event: FraudEvent, key: Option[String] = None): Unit = {
    writeEvent(event, key).fold(
      _ => log.error("[CRITICAL] - Could not write event"),
      _ => resendQueuedEvents()
    )
  }

  def scheduleRetry() = {
    log.debug("************ scheduling retry")
    context.system.scheduler.scheduleOnce(retryDelay, context.parent, RetrySendEvents)
  }

  def writeEvent(event: FraudEvent, key: Option[String] = None): Either[ApiError, WriteSuccess] = {
    val queuedEvent = QueuedEvent(
      _id = UUID.randomUUID().toString,
      topic = eventProducer.getTopic(),
      seq = nextSequence,
      event = event,
      key = key
    )

    queuedEventRepository.save(queuedEvent).fold(
      error => {
        log.error("[CRITICAL] - Could not write queued event")
        Left(error)
      },
      _ => {
        queuedEvents :+= queuedEvent
        Right(WriteSuccess())
      }
    )
  }

  def removeEvent(event: QueuedEvent): Either[ApiError, RemoveSuccess] = {
    queuedEventRepository.delete(event._id).fold(
      error => {
        log.error("[CRITICAL] - Could not delete queued event")
        Left(error)
      },
      _ => {
        queuedEvents = queuedEvents.filterNot(_._id == event._id)
        Right(RemoveSuccess())
      })
  }

  def resendQueuedEvents(): Unit = {
    Logger.info("************* Resending queue'd events")
    if (sendQueuedEvents(queuedEvents)) context become receive else scheduleRetry()
  }

  @tailrec
  private def sendQueuedEvents(events: Seq[QueuedEvent], sent: Option[Boolean] = None): Boolean = {
    (sent, events) match {
      case (Some(s), _) => s
      case (None, Nil) => true
      case (None, head +: tail) =>
        if (!eventProducer.syncSend(head.event.toString(), head.key.orNull)) false
        else {
          sendQueuedEvents(tail, removeEvent(head).fold(
            _ => Some(false),
            _ => None
          ))
        }
    }
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("*********************** Starting event sender actor")

    queuedEventRepository.retrieveAllQueuedEvents().fold(
      _ => log.error("[CRITICAL] - Could not load queued events"),
      result => {
        result.map { events =>
          context become whenQueuedEvents
          queuedEvents = events
          scheduleRetry()
        }
      }
    )

    super.preStart()
  }
}

object EventSenderActor {
  def props(eventProducer: EventProducer,
            queuedEventRepository: QueuedEventRepository,
            retryDelay: FiniteDuration = 5 seconds): Props =
    Props.create(classOf[EventSenderActor], eventProducer, queuedEventRepository, retryDelay)

  def supervisorProps(queuedEventRepository: QueuedEventRepository,
                      retryDelay: FiniteDuration = 5 seconds): Props =
    Props.create(classOf[EventSenderActorSupervisor], queuedEventRepository, retryDelay)

  // Commands
  case object RetrySendEvents

  case object Start

  case class WriteSuccess()

  case class RemoveSuccess()

}
