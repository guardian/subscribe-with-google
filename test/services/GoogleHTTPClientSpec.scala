package services

import mockws.MockWS
import model._
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{Matchers, WordSpecLike}

import play.api.Configuration
import play.api.mvc.Action
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class GoogleHTTPClientSpec extends WordSpecLike with Matchers with ScalaFutures {
  "SKUs" must {

    val configuration = Configuration.from(
      Map(
        "google.packageName" -> "somePackageName",
        "google.apiUrl" -> "http://someMockUrl",
        "google.playDeveloperAccessToken" -> "someAccessToken"
      )
    )

    val timeout = Timeout(Span(500, Millis))
    val interval = Interval(Span(25, Millis))

    "Retrieve and deserialise a SKU" in {
      val ws = MockWS {
        case (
            GET,
            "http://someMockUrl/somePackageName/inappproducts/skuCode"
            ) =>
          Action {
            Ok(s"""{"packageName": "packageName",
                 |"sku": "skuCode",
                 |"status": "status",
                 |"purchaseType": "subscription",
                 |"defaultPrice": {"priceMicros": "2500000", "currency": "GBP"},
                 |"prices": {"GB": {"priceMicros": "2500000", "currency": "GBP"}},
                 |"listings": {"en-GB": {"title": "title", "description": "description"}},
                 |"defaultLanguage": "default language",
                 |"subscriptionPeriod": "P1M",
                 |"season": {"start": {"month":1, "day":1}, "end": {"month":1, "day":1}, "prorations": [{"start": {"month":1, "day":1}, "defaultPrice": {"priceMicros": "2500000", "currency": "GBP"}}]},
                 |"trialPeriod": "P5D"}""".stripMargin)
          }
      }

      val googleHttpClient = new GoogleHTTPClient(ws, configuration)

      whenReady(googleHttpClient.getSKU("skuCode"), timeout, interval) { result =>
        result.right.get shouldBe SKU(
          "packageName",
          "skuCode",
          "status",
          "subscription",
          Price("2500000", "GBP"),
          Map("GB" -> Price("2500000", "GBP")),
          Map("en-GB" -> Listing("title", "description")),
          "default language",
          "P1M",
          Season(
            SeasonDate(1, 1),
            SeasonDate(1, 1),
            Some(List(Proration(SeasonDate(1, 1), Price("2500000", "GBP"))))
          ),
          "P5D"
        )
      }
    }

    "Fail with invalid JSON" in {
      val ws = MockWS {
        case (
            GET,
            "http://someMockUrl/somePackageName/inappproducts/skuCode"
            ) =>
          Action {
            Ok(s"""{"packageName": "packageName"}""")
          }
      }

      val googleHttpClient = new GoogleHTTPClient(ws, configuration)

      whenReady(googleHttpClient.getSKU("skuCode"), timeout, interval) { result =>
        result.left.get shouldBe a[GoogleHTTPClientDeserialisationException]
      }
    }

    "Fail if HTTP request fails" in {
      val ws = MockWS {
        case (
            GET,
            "http://someMockUrl/somePackageName/inappproducts/skuCode"
            ) =>
          Action {
            InternalServerError("Some Server Error")
          }
      }

      val googleHttpClient = new GoogleHTTPClient(ws, configuration)

      whenReady(googleHttpClient.getSKU("skuCode")) { result =>
        result.left.get shouldBe GoogleHTTPClientException(
          INTERNAL_SERVER_ERROR,
          "Server error"
        )
      }
    }
  }

  "Subscription Purchases" must {

    val configuration = Configuration.from(
      Map(
        "google.packageName" -> "somePackageName",
        "google.apiUrl" -> "http://someMockUrl",
        "google.playDeveloperAccessToken" -> "someAccessToken"
      )
    )

    val timeout = Timeout(Span(500, Millis))
    val interval = Interval(Span(25, Millis))

    "Retrieve and deserialise a subscription purchase" in {
      val ws = MockWS {
        case (
          GET,
          "http://someMockUrl/somePackageName/purchases/subscriptions/someProductId/tokens/somePurchaseToken"
          ) =>
          Action {
            Ok(s"""{
                  |"kind": "androidpublisher#subscriptionPurchase",
                  |"startTimeMillis": 1,
                  |"expiryTimeMillis": 1,
                  |"autoRenewing": true,
                  |"priceCurrencyCode": "GBP",
                  |"priceAmountMicros": 1,
                  |"countryCode": "en-GB",
                  |"developerPayload": "devPayload",
                  |"paymentState": 1,
                  |"cancelReason": 1,
                  |"userCancellationTimeMillis": 1,
                  |"cancelSurveyResult": {
                  | "cancelSurveyReason": 1,
                  | "userInputCancelReason": "reason"
                  |},
                  |"orderId": "orderId",
                  |"linkedPurchaseToken": "linkedPurchaseToken",
                  |"purchaseType": 1,
                  |"profileName": "profileName",
                  |"emailAddress": "email",
                  |"givenName": "givenName",
                  |"familyName": "familyName",
                  |"profileId": "profileId"
                  |}""".stripMargin)
          }
      }

      val googleHttpClient = new GoogleHTTPClient(ws, configuration)

      whenReady(googleHttpClient.getSubscriptionPurchase("someProductId", "somePurchaseToken"), timeout, interval) { result =>
        result.right.get shouldBe SubscriptionPurchase(
          "androidpublisher#subscriptionPurchase",
          1,
          1,
          true,
          "GBP",
          1,
          "en-GB",
          "devPayload",
          1,
          1,
          1,
          CancelSurveyResult(1, "reason"),
          "orderId",
          "linkedPurchaseToken",
          1,
          "profileName",
          "email",
          "givenName",
          "familyName",
          "profileId",
        )
      }
    }

    "Fail with invalid JSON" in {
      val ws = MockWS {
        case (
          GET,
          "http://someMockUrl/somePackageName/purchases/subscriptions/someProductId/tokens/somePurchaseToken"
          ) =>
          Action {
            Ok(s"""{"packageName": "packageName"}""")
          }
      }

      val googleHttpClient = new GoogleHTTPClient(ws, configuration)

      whenReady(googleHttpClient.getSubscriptionPurchase("someProductId", "somePurchaseToken"), timeout, interval) { result =>
        result.left.get shouldBe a[GoogleHTTPClientDeserialisationException]
      }
    }

    "Fail if HTTP request fails" in {
      val ws = MockWS {
        case (
          GET,
          "http://someMockUrl/somePackageName/purchases/subscriptions/someProductId/tokens/somePurchaseToken"
          ) =>
          Action {
            InternalServerError("Some Server Error")
          }
      }

      val subPurchaseLookup = new GoogleHTTPClient(ws, configuration)

      whenReady(subPurchaseLookup.getSubscriptionPurchase("someProductId", "somePurchaseToken")) { result =>
        result.left.get shouldBe GoogleHTTPClientException(
          INTERNAL_SERVER_ERROR,
          "Server error"
        )
      }
    }
  }
}
