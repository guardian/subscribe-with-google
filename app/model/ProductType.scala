package model

import enumeratum.EnumEntry.LowerCamelcase
import enumeratum._

sealed trait ProductType extends EnumEntry with LowerCamelcase

object ProductType extends PlayEnum[ProductType] {
  val values = findValues

  case object Single extends ProductType
  case object Recurring extends ProductType
}

