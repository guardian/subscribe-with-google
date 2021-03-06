package model

import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{JsError, JsResult, Json}

class DeveloperNotificationSpec extends WordSpecLike with Matchers {

  "Developer Notification" must {
    "Serialize a developer subscription notification" in {

      val subscriptionNotification = SubscriptionNotification("1.0",
        NotificationType.SubscriptionRenewed,
        "",
        "uk.co.guardian.subscription")

      val currentTime = System.currentTimeMillis()
      val subscriptionDeveloperNotification: DeveloperNotification = SubscriptionDeveloperNotification(
        "1.0",
        "com.guardian",
        1549369095263L,
        subscriptionNotification)

      val jsValue = Json.parse("""{"version":"1.0","packageName":"com.guardian","eventTimeMillis":"1549369095263","subscriptionNotification":{"version":"1.0","notificationType":2,"purchaseToken":"","subscriptionId":"uk.co.guardian.subscription"}}""")
      Json.stringify(Json.toJson[DeveloperNotification](subscriptionDeveloperNotification)) shouldBe Json.stringify(jsValue)
    }

    "Fail to serialize a developer subscription notification with a non-enum notification type" in {

      val subscriptionNotification = SubscriptionNotification("1.0",
        NotificationType.SubscriptionRenewed,
        "",
        "uk.co.guardian.subscription")

      val currentTime = System.currentTimeMillis()
      val subscriptionDeveloperNotification: DeveloperNotification = SubscriptionDeveloperNotification(
        "1.0",
        "com.guardian",
        1549369095263L,
        subscriptionNotification)

      val jsValue = Json.parse("""{"version":"1.0","packageName":"com.guardian","eventTimeMillis":"1549369095263","subscriptionNotification":{"version":"1.0","notificationType":1337,"purchaseToken":"","subscriptionId":"uk.co.guardian.subscription"}}""")

      val notification = jsValue.validate[DeveloperNotification]

      notification shouldBe a[JsError]
    }

    "Serialize a developer test notification" in {

      val testNotification = TestNotification("1.0")

      val currentTime = System.currentTimeMillis()
      val testDeveloperNotification: DeveloperNotification = TestDeveloperNotification(
        "1.0",
        "com.gu",
        currentTime,
        testNotification)


      val jsValue = Json.parse(
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":"$currentTime",
           |"testNotification":{
           |"version":"1.0"
           |}
           |}""".stripMargin
      )

      Json.toJson[DeveloperNotification](testDeveloperNotification) shouldBe jsValue

    }

    "Deserialize a developer test notification" in {
      val testNotification = TestNotification("1.0")

      val currentTime = System.currentTimeMillis()
      val testDeveloperNotification: DeveloperNotification = TestDeveloperNotification(
        "1.0",
        "com.gu",
        currentTime,
        testNotification)


      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":"$currentTime",
           |"testNotification":{
           |"version":"1.0"
           |}
           |}""".stripMargin


      Json.fromJson[DeveloperNotification](Json.parse(jsonString)).get shouldBe testDeveloperNotification
    }

    "Deserialize a developer subscription notification" in {

      val subscriptionNotification = SubscriptionNotification("1.0",
        NotificationType.SubscriptionInGracePeriod,
        "purchaseID",
        "sku")

      val currentTime = System.currentTimeMillis()
      val subscriptionDeveloperNotification: DeveloperNotification = SubscriptionDeveloperNotification(
        "1.0",
        "com.gu",
        currentTime,
        subscriptionNotification)


      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":"$currentTime",
           |"subscriptionNotification":{
           |"version":"1.0",
           |"notificationType":6,
           |"purchaseToken":"purchaseID",
           |"subscriptionId":"sku"}}""".stripMargin


      Json.fromJson[DeveloperNotification](Json.parse(jsonString)).get shouldBe subscriptionDeveloperNotification
    }

    "Error on bad json with both keys" in {
      val currentTime = System.currentTimeMillis()

      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":"$currentTime",
           |"subscriptionNotification":{
           |"version":"1.0",
           |"notificationType":6,
           |"purchaseToken":"purchaseID",
           |"subscriptionId":"sku"},
           |"testNotification":{
           |"version":"1.0"
           |}}""".stripMargin

      Json.fromJson[DeveloperNotification](Json.parse(jsonString)) shouldBe a[JsError]

      getFirstJsValidationError(Json.fromJson[DeveloperNotification](Json.parse(jsonString)))
        .get.message should include("subscription and test data")
    }

    "Error on bad json with no keys" in {
      val currentTime = System.currentTimeMillis()

      val jsonString =
        s"""{"version":"1.0",
           |"packageName":"com.gu",
           |"eventTimeMillis":"$currentTime"
           |}""".stripMargin

      Json.fromJson[DeveloperNotification](Json.parse(jsonString)) shouldBe a[JsError]

      getFirstJsValidationError(Json.fromJson[DeveloperNotification](Json.parse(jsonString)))
        .get.message should include("not contain any notification")
    }
  }

  //Please forgive me.
  private def getFirstJsValidationError[A](jsRes: JsResult[A]) = {
    jsRes.asEither.swap.map(l => l.headOption.flatMap(h => h._2.headOption)).toOption.flatten
  }
}
