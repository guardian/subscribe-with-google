package model

sealed trait NotificationType {

  val notificationType: Int

}

//(9) SUBSCRIPTION_DEFERRED - A subscription's recurrence time has been extended.

/**
  * (1) SUBSCRIPTION_RECOVERED - A subscription was recovered from account hold.
  *
  * @param notificationType
  */
case class SubscriptionRecovered(notificationType: Int) extends NotificationType

/**
  * (2) SUBSCRIPTION_RENEWED - An active subscription was renewed.
  *
  * @param notificationType
  */
case class SubscriptionRenewed(notificationType: Int) extends NotificationType

/**
  * //(3) SUBSCRIPTION_CANCELED - A subscription was either voluntarily or involuntarily cancelled.
  * For voluntary cancellation, sent when the user cancels.
  *
  * @param notificationType
  */
case class SubscriptionCanceled(notificationType: Int) extends NotificationType


/**
  * (4) SUBSCRIPTION_PURCHASED - A new subscription was purchased.
  *
  * @param notificationType
  */
case class SubscriptionPurchased(notificationType: Int) extends NotificationType

/**
  * (5) SUBSCRIPTION_ON_HOLD - A subscription has entered account hold (if enabled).
  *
  * @param notificationType
  */
case class SubscriptionOnHold(notificationType: Int) extends NotificationType

/**
  * (6) SUBSCRIPTION_IN_GRACE_PERIOD - A subscription has entered grace period (if enabled).
  *
  * @param notificationType
  */
case class SubscriptionInGracePeriod(notificationType: Int) extends NotificationType

/**
  * (7) SUBSCRIPTION_RESTARTED - User has reactivated their subscription from Play > Account > Subscriptions
  * (requires opt-in for subscription restoration)
  *
  * @param notificationType
  */
case class SubscriptionRestarted(notificationType: Int) extends NotificationType


//(8) SUBSCRIPTION_PRICE_CHANGE_CONFIRMED - A subscription price change has successfully been confirmed by the user.
/**
  * (8) SUBSCRIPTION_PRICE_CHANGE_CONFIRMED - A subscription price change has successfully been confirmed by the user.
  *
  * @param notificationType
  */
case class SubscriptionPriceChangeConfirmed(notificationType: Int) extends NotificationType


/**
  * (9) SUBSCRIPTION_DEFERRED - A subscription's recurrence time has been extended.
  *
  * @param notificationType
  */
case class SubscriptionDeferred(notificationType: Int) extends NotificationType

