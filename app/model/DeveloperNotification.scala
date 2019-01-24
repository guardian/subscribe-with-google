package model


//todo: Improve naming
sealed trait IncomingGoogleEvent {
  val version: String
  val packageName: String
  val eventTimeMillis: Long
}


case class IncomingSubscriptionEvent(version: String,
                                 packageName: String,
                                 eventTimeMillis: Long,
                                 subscriptionNotification: SubscriptionNotification
                                ) extends IncomingGoogleEvent


case class IncomingTestEvent(
                             version: String,
                             packageName: String,
                             eventTimeMillis: Long,
                             testNotification: TestNotification
                            ) extends IncomingGoogleEvent