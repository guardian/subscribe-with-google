package model

sealed trait Notification {
  val version: String
}

case class SubscriptionNotification(version: String,
                                    notificationType: Int, //todo: Enumeration or atleast factory to meaningful type
                                    purchaseToken: String,
                                    subscriptionId: String) extends Notification {}

case class TestNotification(version: String) extends Notification {}

