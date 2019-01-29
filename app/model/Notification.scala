package model

import play.api.libs.json.{Format, JsResult, JsValue, Json}


//No need to deserialize based on trait type as specificity is determined by DeveloperNotification type
sealed trait Notification {
  val version: String
}

case class SubscriptionNotification(version: String,
                                    notificationType: NotificationType,
                                    purchaseToken: String,
                                    subscriptionId: String) extends Notification {}

case class TestNotification(version: String) extends Notification {}


object SubscriptionNotification {
  implicit val format = Json.format[SubscriptionNotification]
}

object TestNotification {
  implicit val format = Json.format[TestNotification]
}