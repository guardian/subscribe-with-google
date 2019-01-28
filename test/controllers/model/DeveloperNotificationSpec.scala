package controllers.model

import model._
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{JsError, JsResult, Json}

class DeveloperNotificationSpec extends WordSpecLike with Matchers {

  "Developer Notification" must {
    "Serialize a subscription event" in {

      val subscriptionNotification = SubscriptionNotification("1.0",
        NotificationType.SubscriptionPurchased,
        "purchaseID",
        "sku")

      val currentTime = System.currentTimeMillis()
      val subscriptionEvent: IncomingGoogleEvent = IncomingSubscriptionEvent(
        "1.0",
        "com.gu",
        currentTime,
        subscriptionNotification)


      val jsValue = Json.parse(
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":${currentTime},
           |"subscriptionNotification":{
           |"version":"1.0",
           |"notificationType":4,
           |"purchaseToken":"purchaseID",
           |"subscriptionId":"sku"}}""".stripMargin
      )

      Json.toJson[IncomingGoogleEvent](subscriptionEvent) shouldBe jsValue

    }

    "Serialize a test event" in {

      val testNotification = TestNotification("1.0")

      val currentTime = System.currentTimeMillis()
      val incomingTestEvent: IncomingGoogleEvent = IncomingTestEvent(
        "1.0",
        "com.gu",
        currentTime,
        testNotification)


      val jsValue = Json.parse(
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":${currentTime},
           |"testNotification":{
           |"version":"1.0"
           |}
           |}""".stripMargin
      )

      Json.toJson[IncomingGoogleEvent](incomingTestEvent) shouldBe jsValue

    }

    "Deserialize a test event" in {
      val testNotification = TestNotification("1.0")

      val currentTime = System.currentTimeMillis()
      val incomingTestEvent: IncomingGoogleEvent = IncomingTestEvent(
        "1.0",
        "com.gu",
        currentTime,
        testNotification)


      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":${currentTime},
           |"testNotification":{
           |"version":"1.0"
           |}
           |}""".stripMargin


      Json.fromJson[IncomingGoogleEvent](Json.parse(jsonString)).get shouldBe incomingTestEvent
    }

    "Deserialize a subscription event" in {

      val subscriptionNotification = SubscriptionNotification("1.0",
        NotificationType.SubscriptionInGracePeriod,
        "purchaseID",
        "sku")

      val currentTime = System.currentTimeMillis()
      val subscriptionEvent: IncomingGoogleEvent = IncomingSubscriptionEvent(
        "1.0",
        "com.gu",
        currentTime,
        subscriptionNotification)


      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":${currentTime},
           |"subscriptionNotification":{
           |"version":"1.0",
           |"notificationType":6,
           |"purchaseToken":"purchaseID",
           |"subscriptionId":"sku"}}""".stripMargin


      Json.fromJson[IncomingGoogleEvent](Json.parse(jsonString)).get shouldBe subscriptionEvent
    }

    "Error on bad json with both keys" in {
      val currentTime = System.currentTimeMillis()

      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":${currentTime},
           |"subscriptionNotification":{
           |"version":"1.0",
           |"notificationType":6,
           |"purchaseToken":"purchaseID",
           |"subscriptionId":"sku"},
           |"testNotification":{
           |"version":"1.0"
           |}}""".stripMargin

      Json.fromJson[IncomingGoogleEvent](Json.parse(jsonString)) shouldBe a[JsError]

      getFirstJsValidationError(Json.fromJson[IncomingGoogleEvent](Json.parse(jsonString)))
        .get.message should include("subscription and test data")
    }

    "Error on bad json with no keys" in {
      val currentTime = System.currentTimeMillis()

      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":${currentTime}
           |}""".stripMargin

      Json.fromJson[IncomingGoogleEvent](Json.parse(jsonString)) shouldBe a[JsError]

      getFirstJsValidationError(Json.fromJson[IncomingGoogleEvent](Json.parse(jsonString)))
        .get.message should include("not contain any notification")
    }
  }

  //Please forgive me.
  private def getFirstJsValidationError[A](jsRes: JsResult[A]) = {
    jsRes.asEither.swap.map(l => l.headOption.flatMap(h => h._2.headOption)).toOption.flatten
  }
}
