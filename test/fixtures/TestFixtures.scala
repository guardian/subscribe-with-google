package fixtures

import java.util.Base64

import model._
import play.api.libs.json.Json

object TestFixtures {


  val subscriptionNotification = SubscriptionNotification("1.0",
    NotificationType.SubscriptionPurchased,
    "purchaseToken",
    "skuID")

  val developerNotificationWithSubscription: DeveloperNotification = SubscriptionDeveloperNotification("1.0",
    "com.gu",
    1L,
    subscriptionNotification
  )

  val googlePushMessage = GooglePushMessage(Map.empty[String, String],
    new String(Base64.getEncoder.encode(Json.asciiStringify(
    Json.toJson(developerNotificationWithSubscription)
    ).getBytes)),
    "messageId")

  val googlePushMessageWithInvalidBody = GooglePushMessage(Map.empty[String, String],
    new String(Base64.getEncoder.encode(Json.asciiStringify(
      Json.toJson("{}")
    ).getBytes)),
    "messageId")

  val googlePushMessageWrapper = GooglePushMessageWrapper(googlePushMessage, "subscription")

}
