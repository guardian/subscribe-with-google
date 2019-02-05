package model

sealed trait SKUType {}

case object OneTimeContribution extends SKUType

case object RecurringContribution extends SKUType

