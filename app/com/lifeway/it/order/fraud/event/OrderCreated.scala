package com.lifeway.it.order.fraud.event

import org.json4s.JsonAST.{JNothing, JObject, JValue}
import org.json4s._

case class OrderCreated (id: String, timestamp: String, payload: Order) extends Event

case class Order (id: String,
                  orderNumber: String,
                  details: OrderDetails
                 ) {
  def shippingAddress:Option[Address] = {
    // This assumes there is only one shipment that contains an address. Shipments without an address are typically for digital products.
    details.shipments.filter(_.shippingAddress.isDefined).headOption match {
      case Some(shipment:Shipment) => shipment.shippingAddress
      case _ => None
    }
  }

  def billingAddress:Option[Address] = {
    // This assumes there is only one payment that contains an address.
    details.payments.filter(_.paymentMethod.isInstanceOf[CreditCardPayment]).headOption match {
      case Some(payment:Payment) => Some(payment.paymentMethod.asInstanceOf[CreditCardPayment].address)
      case _ => None
    }
  }

  def orderTotal:Double = {
    details.header.grandTotal
  }

  def creditCardType:Option[String] = {
    details.payments.filter(_.paymentMethod.isInstanceOf[CreditCardPayment]).headOption match {
      case Some(payment:Payment) => Some(payment.paymentMethod.asInstanceOf[CreditCardPayment].cardType)
      case _ => None
    }
  }
}

case class OrderDetails (orderNumber: String,
                         displayOrderId: Option[String],
                         header: OrderHeader,
                         appliedPromotions: Option[Seq[Promotion]],
                         customerReference: Option[String],
                         customer: Customer,
                         associatedOrganization: Option[Organization],
                         shipments: Seq[Shipment],
                         payments: Seq[Payment],
                         giftCardPayments: Option[Seq[GiftCardPayment]],
                         orderLink: Option[String],
                         confirmationEmails: Option[Seq[String]]
                        )

case class OrderHeader (orderStatus: String,
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
                       )

case class Organization (billingAccountId: String)

case class Promotion (code: String,
                      name: String,
                      description: Option[String],
                      `type`: Option[String],
                      discountAmount: Option[Double],
                      discountedQuantity: Option[Int],
                      appliedCoupons: Option[List[Coupon]]
                     )

case class Coupon (code: String,
                   currentUsageCount: Int
                  )

case class GiftCardPayment (cardNumber: String,
                            authToken: String,
                            chargedAmount: Price
                           )

case class Shipment (shipmentId: String,
                     shipmentStatus: String,
                     shippingCost: Option[Double],
                     discountAmount: Double,
                     shippingAddress: Option[Address],
                     shipmentType: String,
                     shipmentCompleteData: Option[String],
                     shipmentCarrier: Option[String],
                     trackingReference: Option[String],
                     lineItems: List[LineItem],
                     shippingMethodDescription: Option[String]
                    )

case class LineItem (lineItemId: String,
                     appliedPromotions: Option[Seq[Promotion]],
                     parentLineItemId: Option[String],
                     productCode: String,
                     itemNumber: String,
                     quantity: Int,
                     listUnitPrice: Double,
                     saleUnitPrice: Option[Double],
                     unitPrice: Double,
                     itemSubtotalPrice: Double,
                     amountBeforeTax: Double,
                     itemTaxes: Double,
                     amountIncludingTax: Double,
                     distributionType: Option[String],
                     distributionAction: Option[String],
                     displayName: String,
                     options: Option[List[KeyValuePair]],
                     taxLines: List[TaxLine],
                     productUrl: Option[String]
                    )

case class KeyValuePair(key: String, value: String, displayValue: Option[String])

case class TaxLine (jurisdictionId: String,
                    taxRegionId: String,
                    taxIsInclusive: Boolean,
                    taxName: String,
                    taxCode: String,
                    taxAmount: Double,
                    taxRate: Double,
                    taxCalculationDate: String
                   )

case class Price(amount: BigDecimal, currency: String, display: String)

case class Payment (paymentStatus: String,
                    paymentMethod: PaymentMethod
                   )

sealed trait PaymentMethod {
  def id: String
}

case class CreditCardPayment(id: String,
                             cardType: String,
                             token: String,
                             cardholderName: String,
                             expMonth: Int,
                             expYear: Int,
                             address: Address,
                             amount: Double,
                             authCode: String
                            ) extends PaymentMethod

case class BillingAccountPayment(id: String,
                                 organizationId: String,
                                 billingAccountId: String,
                                 accountNumber: String,
                                 address: Address,
                                 name: String,
                                 amount: Double
                                ) extends PaymentMethod


object paymentSerializer {

  object PaymentCodec extends CustomSerializer[PaymentMethod](implicit format => ( {
    case x: JObject =>
      if (x.has("cardType")) {
        val token = (x \ "token").extract[String]
        val cardholderName = (x \ "cardholderName").extract[String]
        val expMonth = (x \ "expMonth").extract[Int]
        val expYear = (x \ "expYear").extract[Int]
        val authCode = (x \ "authCode").extract[String]
        val amount = (x \ "amount").extract[Double]
        val id = (x \ "id").extract[String]
        val cardType = (x \ "cardType").extract[String]
        val address = (x \ "address").extract[Address]

        CreditCardPayment(id, cardType, token, cardholderName, expMonth, expYear, address, amount, authCode)
      } else {
        val organizationId = (x \ "organizationId").extract[String]
        val billingAccountId = (x \ "billingAccountId").extract[String]
        val accountNumber = (x \ "accountNumber").extract[String]
        val address = (x \ "address").extract[Address]
        val name = (x \ "name").extract[String]
        val amount = (x \ "amount").extract[Double]
        val id = (x \ "id").extract[String]

        BillingAccountPayment(id, organizationId, billingAccountId, accountNumber, address, name, amount)
      }
  }, {
    case x: PaymentMethod =>
      JNothing
  }
  ))

  implicit class JValueExtended(value: JValue) {
    def has(childString: String): Boolean = {
      if ((value \ childString) != JNothing) {
        true
      } else {
        false
      }
    }
  }
}