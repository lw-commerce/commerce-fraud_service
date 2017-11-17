package com.lifeway.it.order.fraud

import com.typesafe.config.{Config, ConfigFactory}

/*
fraud {
  messaging {
    subscribedTopics = "publicOrderProcessing"
    publishTopic = "publicFraud"

    mongo {
      uri = "mongo:27017"
      db = "fraudEvents"
      sslInvalidHostNameAllowed = "false"
    }

    kafka {
      groupId = "fraudService"
      parallelism = 9
      batchSize = 250
      brokers = "localhost:9092"
    }
  }
 */

object AppConfiguration {
  val config: Config = ConfigFactory.load.getConfig("fraud")

  println(config)

  // General
  def subscribedTopics = config.getString("messaging.subscribedTopics").split(",").map(_.trim).toSeq
  def publishTopic = config.getString("messaging.publishTopic")

  // Mongo
  def mongoUri = s"mongodb://${config.getString("messaging.mongo.hosts")}"
  def db = config.getString("messaging.mongo.db")
  def sslInvalidHostNameAllowed = config.getBoolean("messaging.mongo.sslInvalidHostNameAllowed")

  // Kafka
  def kafkaGroupId = config.getString("messaging.kafka.groupId")
  def kafkaParallelism = config.getInt("messaging.kafka.parallelism")
  def kafkaBatchSize = config.getInt("messaging.kafka.batchSize")
  def kafkaBrokers = config.getString("messaging.kafka.brokers")
}
