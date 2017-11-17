package com.lifeway.it.order.fraud.event

case class Address (id: String,
                    firstName: String,
                    lastName: String,
                    line1: String,
                    line2: Option[String],
                    city: String,
                    state: String,
                    zip: String,
                    country: String,
                    organization: Option[String],
                    phone: Option[String],
                    addressType: Option[String],
                    save: Option[Boolean],
                    default: Option[Boolean]
                   ) {


  def canEqual(a: Any) = a.isInstanceOf[Address]

  override def equals(that: Any): Boolean =
    that match {
      case that: Address => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + line1.hashCode
    line2 match {
      case Some(s) => result = prime * result + s.hashCode
      case _ =>
    }

    result = prime * result + city.hashCode
    result = prime * result + state.hashCode
    result = prime * result + zip.hashCode
    result = prime * result + country.hashCode

    return result
  }
}

