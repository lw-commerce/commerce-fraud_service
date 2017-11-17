package com.lifeway.it.order.fraud.event

case class Customer(customerId: String,
                    customerUserName: String,
                    firstName: String,
                    lastName: String,
                    email: String
                    )
