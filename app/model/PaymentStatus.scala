package model

import enumeratum.{ PlayJsonEnum, Enum, EnumEntry }

sealed trait PaymentStatus extends EnumEntry

object PaymentStatus extends Enum[PaymentStatus] with PlayJsonEnum[PaymentStatus] {
  val values = findValues

  case object Paid extends PaymentStatus

  case object Refunded extends PaymentStatus
}
