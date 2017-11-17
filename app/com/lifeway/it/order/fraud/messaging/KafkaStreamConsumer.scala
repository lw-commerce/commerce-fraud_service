package com.lifeway.it.order.fraud.messaging

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import com.lifeway.it.order.fraud.messaging.KafkaStreamConsumer.{SaveHandledEvents, StartStream, StopStream}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

abstract class KafkaStreamConsumer(repository: KafkaStreamStateRepository) extends Actor with ActorLogging {
  implicit val materializer: Materializer = ActorMaterializer.create(context.system)

  val streamId = UUID.randomUUID().toString

  var handledEvents: Seq[String] = Seq.empty[String]

  def handleKafkaMessage(key: String, topic: String, message: String): Unit

  override def receive = {
    case cmd: StartStream => startStream(cmd)
    case SaveHandledEvents => saveHandledEvents
    case _: Any => //ignore
  }

  def whenRunning(control: Control): Receive = {
    case StopStream =>
      Logger.debug("Shutting down stream")
      control.shutdown().andThen {
        case _ =>
          context become receive
      }

    case SaveHandledEvents => saveHandledEvents
    case _: Any => //ignore
  }

  def saveHandledEvents = {
    //TODO: do this w/ CRDTS? Depend on in memory for now// repository.save(KafkaStreamState(self.path.name, handledEvents))
  }

  def startStream(start: StartStream) = {
    Logger.info(s"Starting kafka stream for topics: ${start.topics}")

    val consumerSettings = ConsumerSettings(context.system, new StringDeserializer, new StringDeserializer)
        .withBootstrapServers(start.brokers)
        .withGroupId(start.groupId)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val (control, future) = Consumer.committableSource(consumerSettings, Subscriptions.topics(start.topics.toSet))
        .mapAsync(1) { msg =>
          debug(s"received....  ${msg.record.value()}, offset = ${msg.record.offset()}, partition ${msg.record.partition()}")
          handleKafkaMessage(msg.record.key(), msg.record.topic(), msg.record.value())
          Future {
            msg.committableOffset
          }
        }
        .batch(max = start.batchSize, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) => batch.updated(elem) }
        .mapAsync(3)(_.commitScaladsl())
        .toMat(Sink.ignore)(Keep.both)
        .run()

    future.onFailure {
      case ex =>
        Logger.error("Stream failed due to error. Shutting down.", ex)
        self ! StopStream
    }

    context become whenRunning(control)
  }

  def notAlreadyHandled(id: String) = !handledEvents.contains(id)

  def markEventHandled(id: String): Unit = {
    handledEvents = if (handledEvents.size > 10000) handledEvents.drop(1) :+ id else handledEvents :+ id
  }

  override def preStart(): Unit = {
    repository.find(self.path.name).fold(
      _ => Logger.error("Unable to load kafka stream consumer state"), {
        case Some(events) => handledEvents = events.eventIds
        case None => ()
      }
    )

    context.system.scheduler.schedule(0 seconds, 5 minutes, self, SaveHandledEvents)

    super.preStart()
  }

  def debug(msg: String) = Logger.debug(s"***************************************** $msg")
}

object KafkaStreamConsumer {
  val shardName = "KafkaStreamConsumer"

  def props(repository: KafkaStreamStateRepository) = Props(classOf[KafkaStreamConsumer], repository)

  case class StartStream(groupId: String,
                         partitions: Int,
                         batchSize: Int,
                         brokers: String,
                         topics: Seq[String])

  case object StopStream
  case object SaveHandledEvents

  case class KafkaStreamState(_id: String, eventIds: Seq[String] = Seq.empty[String])

  object KafkaStreamState {
    implicit val stateFmt = Json.format[KafkaStreamState]
  }

}
