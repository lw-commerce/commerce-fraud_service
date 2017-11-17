package com.lifeway.it.order.fraud.messaging

import com.mongodb.MongoClientOptions
import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoCollection, MongoDB}

import scala.collection.Set

trait MongoDBService {
  def getCollection(name: String): MongoCollection
  def getClient():MongoDB
  def collectionNames: Set[String]
}

class DefaultMongoDBService(mongoUri: String, db: String, sslInvalidHostNameAllowed: Boolean) extends MongoDBService {

  val clientOptions = MongoClientOptions.builder().sslInvalidHostNameAllowed(sslInvalidHostNameAllowed)
  val mongoClientURI = new MongoClientURI(new com.mongodb.MongoClientURI(mongoUri, clientOptions))
  val client = MongoClient(mongoClientURI)
  val mongoClient = client.getDB(db)

  override def getCollection(name: String): MongoCollection = mongoClient.apply(name)

  override def collectionNames = mongoClient.collectionNames()

  override def getClient = mongoClient


}