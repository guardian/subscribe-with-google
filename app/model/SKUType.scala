package model

import enumeratum._

sealed trait SKUType extends EnumEntry

object SKUType extends Enum[SKUType] {
  val values = findValues

  case object Single extends SKUType
  case object Recurring extends SKUType
}
