package com.lifeway.it.order.fraud.actor

import com.lifeway.it.order.fraud.AppConfiguration._
import com.lifeway.it.order.fraud.messaging.{KafkaStreamConsumer, KafkaStreamStateRepository, QueuedEventRepository}
import org.slf4j.LoggerFactory
import akka.actor.{ActorRef, ActorSystem, Props}
import com.lifeway.it.order.fraud.AppConfiguration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object FraudActorSystem {

  val log = LoggerFactory.getLogger(FraudActorSystem.getClass)

  val system = ActorSystem("FraudSystem", AppConfiguration.config)

  private var fraudCheckActor = ActorRef.noSender

  private var eventSenderActor = ActorRef.noSender

  def start(kafkaStreamConsumerRepo: KafkaStreamStateRepository, queuedEventRepository: QueuedEventRepository): Unit = {

    val ref = system.actorOf(EventReader.props(kafkaStreamConsumerRepo))

    eventSenderActor = system.actorOf(EventSenderActor.supervisorProps(queuedEventRepository))

    fraudCheckActor = system.actorOf(Props.create(classOf[FraudCheckHandler]))

    system.scheduler.schedule(0 seconds, 5 seconds, ref, KafkaStreamConsumer.StartStream(kafkaGroupId, kafkaParallelism, kafkaBatchSize,
      kafkaBrokers,
      subscribedTopics))

    log.debug("Actor System Started")
  }

  def eventPublisher = eventSenderActor

  def fraudCheckHandler = fraudCheckActor
}
