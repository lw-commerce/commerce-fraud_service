package com.lifeway.it.order.fraud.actor

import akka.actor.Props
import com.lifeway.it.order.fraud.messaging.{KafkaStreamConsumer, KafkaStreamStateRepository}
import com.lifeway.it.order.fraud.util.JsonUtil

class EventReader (kafkaStreamConsumerRepo: KafkaStreamStateRepository) extends KafkaStreamConsumer(kafkaStreamConsumerRepo) {
  override def handleKafkaMessage(key: String, topic: String, message: String): Unit = {
    topic match {
      case "publicOrderProcessing" =>
        JsonUtil.parseJsonEvent(message).fold(
          error => log.error("Error handling kafka message {}, {}", error.toString, message),
          event => FraudActorSystem.fraudCheckHandler ! event
        )
      case _ =>
        log.error(s"Reading from topic that does not have a deserializer: ${topic}")
    }
  }
}

object EventReader {
  def props(kafkaStreamConsumerRepo: KafkaStreamStateRepository): Props = Props(classOf[EventReader], kafkaStreamConsumerRepo)
}
