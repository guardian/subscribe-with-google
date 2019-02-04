package model

import enumeratum.values.{IntEnum, IntEnumEntry, IntPlayJsonValueEnum}

import scala.collection.immutable

sealed abstract class NotificationType(val value: Int, name: String) extends IntEnumEntry

case object NotificationType extends IntEnum[NotificationType] with IntPlayJsonValueEnum[NotificationType] {
  override def values: immutable.IndexedSeq[NotificationType] = findValues

//  (1) SUBSCRIPTION_RECOVERED - A subscription was recovered from account hold.
  case object SubscriptionRecovered extends NotificationType(1, "SUBSCRIPTION_RECOVERED")
  //  (2) SUBSCRIPTION_RENEWED - An active subscription was renewed.
  case object SubscriptionRenewed extends NotificationType(2, "SUBSCRIPTION_RENEWED")
  //  (3) SUBSCRIPTION_CANCELED - A subscription was either voluntarily or involuntarily cancelled.
  //  For voluntary cancellation, sent when the user cancels.
  case object SubscriptionCanceled extends NotificationType(3, "SUBSCRIPTION_CANCELED")
  //  (4) SUBSCRIPTION_PURCHASED - A new subscription was purchased.
  case object SubscriptionPurchased extends NotificationType(4, "SUBSCRIPTION_PURCHASED")
  //  (5) SUBSCRIPTION_ON_HOLD - A subscription has entered account hold (if enabled).
  case object SubscriptionOnHold extends NotificationType(5, "SUBSCRIPTION_ON_HOLD")
  //  (6) SUBSCRIPTION_IN_GRACE_PERIOD - A subscription has entered grace period (if enabled).
  case object SubscriptionInGracePeriod extends NotificationType(6, "SUBSCRIPTION_IN_GRACE_PERIOD")
  //  (7) SUBSCRIPTION_RESTARTED - User has reactivated their subscription from Play > Account > Subscriptions
  //  (requires opt-in for subscription restoration)
  case object SubscriptionRestarted extends NotificationType(7, "SUBSCRIPTION_RESTARTED")
  //  (8) SUBSCRIPTION_PRICE_CHANGE_CONFIRMED - A subscription price change has successfully been confirmed by the user.
  case object SubscriptionPriceChangeConfirmed extends NotificationType(8, "SUBSCRIPTION_PRICE_CHANGE_CONFIRMED")
  //  (9) SUBSCRIPTION_DEFERRED - A subscription's recurrence time has been extended.
  case object SubscriptionDeferred extends NotificationType(9, "SUBSCRIPTION_DEFERRED")
}
