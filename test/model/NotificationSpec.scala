package model

import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.Json

class NotificationSpec extends WordSpecLike with Matchers {

  "Notification" must {
    "when a test notification be serializable" in {
      val testNotification = TestNotification("1.0")

      Json.toJson(testNotification) shouldBe Json.parse("""{"version": "1.0"}""")
    }

    "when a test notification be deserializable" in {
      val testNotification = TestNotification("1.0")

      val asJson = Json.fromJson[TestNotification](Json.parse("""{"version": "1.0"}""")).get


      asJson shouldBe testNotification
    }

    "when a subscription notification be serializable" in {
      val subscriptionNotification = SubscriptionNotification(
        "1.0",
        NotificationType.withValue(1),
        "purchaseString",
        "subId")

      Json.toJson(subscriptionNotification) shouldBe Json.parse("""{"version":"1.0","notificationType":1,"purchaseToken":"purchaseString","subscriptionId":"subId"}""")
    }
    "when a subscription notification be deserializable" in {
      val subscriptionNotification = SubscriptionNotification(
        "1.0",
        NotificationType.SubscriptionPurchased,
        "purchaseString",
        "subId")


      val jsonVal = Json.fromJson[SubscriptionNotification](Json.parse("""{"version":"1.0","notificationType":4,"purchaseToken":"purchaseString","subscriptionId":"subId"}"""))

      jsonVal.get shouldBe subscriptionNotification
    }
  }
}
