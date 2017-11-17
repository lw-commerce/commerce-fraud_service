package com.lifeway.it.order.fraud.event

import java.util.UUID

import com.lifeway.it.order.fraud.service.FraudCheckService
import com.lifeway.it.order.fraud.util.JsonUtil
import com.lifeway.it.order.fraud.service.FraudCheckService
import org.scalatest.WordSpecLike

class FraudCheckServiceSpec extends WordSpecLike{
  val nashvilleAddress = new Address(UUID.randomUUID().toString, "Bob", "Smith", "100 Main St.", None, "Nashville", "TN", "12345", "US", Some("First Baptist Church"), Some("615-123-4567"), None, None, None)

  val atlantaAddress = new Address(UUID.randomUUID().toString, "Mary", "Smith", "101 Main St.", None, "Atlanta", "GA", "12345", "US", None, Some("404-123-4567"), None, None, None)

  val internationalAddress = new Address(UUID.randomUUID().toString, "Susan", "Jones", "245 Kindersley Ave", None, "Mount Royal", "QC", "H3R 1R6", "CA", None, None, None, None, None)

  val lineItem1:LineItem = LineItem(UUID.randomUUID().toString, None, None, "123", "456", 2, 45.23, Some(40.00), 45.23, 45.23, 45.23, 5.00, 50.23, None, None, "A Book", None, List(TaxLine("STATE", "TN", false, "Sales Tax", "70100", 0, 0, "2017-10-29T10:02:20Z")), None)

  val nashvilleShipment = Shipment(UUID.randomUUID().toString, "SHIPPED", Some(0.00), 0.0, Some(nashvilleAddress), "DIGITAL", None, None, None, List(lineItem1), None)

  val internationalShipment = Shipment(UUID.randomUUID().toString, "SHIPPED", Some(0.00), 0.0, Some(internationalAddress), "DIGITAL", None, None, None, List(lineItem1), None)

  val atlantaShipment = Shipment(UUID.randomUUID().toString, "SHIPPED", Some(0.00), 0.0, Some(atlantaAddress), "DIGITAL", None, None, None, List(lineItem1), None)

  val visaPayment = Payment("SUCCESS", CreditCardPayment(UUID.randomUUID().toString, "VISA", "3393939393939", "Karen Smith", 5, 2021, nashvilleAddress, 3.45, "292929292"))

  val amexPayment = Payment("SUCCESS", CreditCardPayment(UUID.randomUUID().toString, "AMEX", "3393939393939", "Karen Smith", 5, 2021, atlantaAddress, 3.45, "292929292"))

  val nashvillePayment = Payment("SUCCESS", CreditCardPayment(UUID.randomUUID().toString, "VISA", "3393939393939", "Karen Smith", 5, 2021, nashvilleAddress, 3.45, "292929292"))

  val atlantaPayment = Payment("SUCCESS", CreditCardPayment(UUID.randomUUID().toString, "AMEX", "3393939393939", "Karen Smith", 5, 2021, atlantaAddress, 3.45, "292929292"))

  val internationalPayment = Payment("SUCCESS", CreditCardPayment(UUID.randomUUID().toString, "AMEX", "3393939393939", "Karen Smith", 5, 2021, internationalAddress, 3.45, "292929292"))

  val customer1 = Customer(UUID.randomUUID().toString, "bsmith", "Bob", "Smith", "bsmith@something.com")
/*
orderStatus: String,
                        storeCode: String,
                        createdDate: String,
                        locale: String,
                        currency: String,
                        emailFrom: String,
                        totalItemCostBeforeTax: Double,
                        totalShippingCostBeforeTax: Double,
                        totalItemTaxes: Double,
                        totalShippingTaxes: Double,
                        totalTaxes: Double,
                        totalItemCostIncludingTax: Double,
                        totalShippingCostIncludingTax: Double,
                        grandTotal: Double
 */
  val orderHeaderUnderFiftyDollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 49.99)
  val orderHeaderOverFiftyDollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 50.01)
  val orderHeaderUnderThirtyDollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 29.99)
  val orderHeaderOverThirtyDollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 30.01)

  val orderHeaderUnder125Dollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 124.99)
  val orderHeaderOver125Dollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 125.01)

  val orderHeaderUnder400Dollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 399.99)
  val orderHeaderOver400Dollars = OrderHeader("Submitted", "lifeway", "2017-10-19T10:02:21Z", "en", "USD", "store-noreply@lifeway.com", 45.00, 4.00, 3.00, 1.00, 5.00, 50.00, 5.00, 400.01)

  /*
  "Shipping and Billing address do not match and over $125"
  "International Order over $30"
  "AMEX and over $50"
  "Order greater than $400"
   */

  "Test Fraud Check Rules" should {
    "Test AMEX and over $50 rule" in {
      val amexOrderOverFiftyDollars = Order("12345", "12345",  OrderDetails(id(), Some("12345"), orderHeaderOverFiftyDollars, None, None, customer1, None, List(nashvilleShipment), List(amexPayment), None, None, None))
      val amexOrderUnderFiftyDollars = Order("12345", "12345",  OrderDetails(id(), Some("12345"), orderHeaderUnderFiftyDollars, None, None, customer1, None, List(nashvilleShipment), List(amexPayment), None, None, None))
      val notAmexOrderOverFiftyDollars = Order("12345", "12345",  OrderDetails(id(), Some("12345"), orderHeaderOverFiftyDollars, None, None, customer1, None, List(nashvilleShipment), List(visaPayment), None, None, None))
      val notAmexOrderUnderFiftyDollars = Order("12345", "12345",  OrderDetails(id(), Some("12345"), orderHeaderUnderFiftyDollars, None, None, customer1, None, List(nashvilleShipment), List(visaPayment), None, None, None))

      // AMEX and UNDER $50
      val result2 = FraudCheckService.checkForFraud(amexOrderUnderFiftyDollars)
      assert(result2.isInstanceOf[FraudCheckPassed])

      // NOT AMEX and OVER $50
      val result3 = FraudCheckService.checkForFraud(notAmexOrderOverFiftyDollars)
      assert(result3.isInstanceOf[FraudCheckPassed])

      // NOT AMEX and UNDER $50
      val result4 = FraudCheckService.checkForFraud(notAmexOrderUnderFiftyDollars)
      assert(result4.isInstanceOf[FraudCheckPassed])

      // AMEX and OVER $50
      val result = FraudCheckService.checkForFraud(amexOrderOverFiftyDollars)
      assert(result.isInstanceOf[FraudCheckFailed])
      assert(result.asInstanceOf[FraudCheckFailed].failedRules.contains("AMEX and over $50"))
    }

    "Test Shipping and Billing Addresses different" in {
      val shippingAndBillingSameUnder125 = Order("12345", "12345", OrderDetails(id(), Some("12345"), orderHeaderUnder125Dollars, None, None, customer1, None, List(nashvilleShipment), List(nashvillePayment), None, None, None))
      val shippingAndBillingSameOver125 = Order("12345", "12345", OrderDetails(id(), Some("12345"), orderHeaderUnder125Dollars, None, None, customer1, None, List(nashvilleShipment), List(nashvillePayment), None, None, None))

      val shippingAndBillingDifferentUnder125 = Order("12345", "12345", OrderDetails(id(), Some("12345"), orderHeaderUnder125Dollars, None, None, customer1, None, List(atlantaShipment), List(nashvillePayment), None, None, None))
      val shippingAndBillingDifferentOver125 = Order("12345", "12345", OrderDetails(id(), Some("12345"), orderHeaderOver125Dollars, None, None, customer1, None, List(atlantaShipment), List(nashvillePayment), None, None, None))

      // Billing and Shipping the same with order under $125
      val result1 = FraudCheckService.checkForFraud(shippingAndBillingSameUnder125)
      assert(result1.isInstanceOf[FraudCheckPassed])

      // Billing and Shipping the same with order over $125
      val result2 = FraudCheckService.checkForFraud(shippingAndBillingSameOver125)
      assert(result2.isInstanceOf[FraudCheckPassed])

      // Billing and Shipping different with order under $125
      val result3 = FraudCheckService.checkForFraud(shippingAndBillingDifferentUnder125)
      assert(result3.isInstanceOf[FraudCheckPassed])

      // Billing and Shipping different with order over $125
      val result4 = FraudCheckService.checkForFraud(shippingAndBillingDifferentOver125)
      assert(result4.isInstanceOf[FraudCheckFailed])
      assert(result4.asInstanceOf[FraudCheckFailed].failedRules.contains("Shipping and Billing address do not match and over $125"))
    }

    "Test International over $30" in {
      val internationalUnder30Dolalrs = Order("12345", "12345",  OrderDetails(id(), Some("12345"), orderHeaderUnderThirtyDollars, None, None, customer1, None, List(internationalShipment), List(visaPayment), None, None, None))
      val internationalOver30Dolalrs = Order("12345", "12345",  OrderDetails(id(), Some("12345"), orderHeaderOverThirtyDollars, None, None, customer1, None, List(internationalShipment), List(visaPayment), None, None, None))


      val result1 = FraudCheckService.checkForFraud(internationalUnder30Dolalrs)
      assert(result1.isInstanceOf[FraudCheckPassed])

      val result2 = FraudCheckService.checkForFraud(internationalOver30Dolalrs)
      assert(result2.isInstanceOf[FraudCheckFailed])
      assert(result2.asInstanceOf[FraudCheckFailed].failedRules.contains("International Order over $30"))
    }

    "Test over $400" in {
      val under400Dollars = Order("12345", "12345", OrderDetails(id(), Some("12345"), orderHeaderUnder400Dollars, None, None, customer1, None, List(nashvilleShipment), List(nashvillePayment), None, None, None))
      val over400Dollars = Order("12345", "12345", OrderDetails(id(), Some("12345"), orderHeaderOver400Dollars, None, None, customer1, None, List(nashvilleShipment), List(nashvillePayment), None, None, None))

      val result1 = FraudCheckService.checkForFraud(under400Dollars)
      assert(result1.isInstanceOf[FraudCheckPassed])

      val result2 = FraudCheckService.checkForFraud(over400Dollars)
      assert(result2.isInstanceOf[FraudCheckFailed])
      assert(result2.asInstanceOf[FraudCheckFailed].failedRules.contains("Order greater than $400"))
    }
  }

  def id() = {
    UUID.randomUUID().toString
  }
}
