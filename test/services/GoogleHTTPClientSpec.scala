package services

import exceptions.{GoogleHTTPClientDeserializationException, GoogleHTTPClientException}
import mockws.MockWS
import model._
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.test.Helpers._
import utils.MockWSHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GoogleHTTPClientSpec extends WordSpecLike with Matchers with ScalaFutures with MockWSHelper {

  class MockAccessTokenClient extends AccessTokenClient {

    def get() =
      Future.successful(
        GoogleAccessToken("someAccessToken", 1, "someScope", "someType")
      )
  }

  val mockAccessTokenClient = new MockAccessTokenClient()

  "SKUs" must {

    val configuration =
      Configuration.from(
        Map(
          "google.packageName" -> "somePackageName",
          "google.api-endpoint" -> "https://www.googleapis.com",
          "google.account-endpoint" -> "https://www.account.google.com"
        ))

    val timeout = Timeout(Span(500, Millis))
    val interval = Interval(Span(25, Millis))

    "Retrieve and deserialise a SKU" in {
      val ws = MockWS {
        case (
            GET,
            "https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/inappproducts/skuCode"
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

      val googleHttpClient =
        new GoogleHTTPClient(ws, mockAccessTokenClient, configuration)

      whenReady(googleHttpClient.getSKU(SKUCode("skuCode")), timeout, interval) { result =>
        result shouldBe SKU(
          "packageName",
          SKUCode("skuCode"),
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
            "https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/inappproducts/skuCode"
            ) =>
          Action {
            Ok(s"""{"packageName": "packageName"}""")
          }
      }

      val googleHttpClient =
        new GoogleHTTPClient(ws, mockAccessTokenClient, configuration)

      whenReady(googleHttpClient.getSKU(SKUCode("skuCode")) failed, timeout, interval) { result =>
        result shouldBe an[GoogleHTTPClientDeserializationException]
      }
    }

    "Fail if HTTP request fails" in {
      val ws = MockWS {
        case (
            GET,
            "https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/inappproducts/skuCode"
            ) =>
          Action {
            InternalServerError("Some Server Error")
          }
      }

      val googleHttpClient =
        new GoogleHTTPClient(ws, mockAccessTokenClient, configuration)

      whenReady(googleHttpClient.getSKU(SKUCode("skuCode")) failed) { result =>
        result shouldBe GoogleHTTPClientException(
          INTERNAL_SERVER_ERROR,
          "Attempt to connect to https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/inappproducts/skuCode - Received status 500"
        )
      }
    }
  }

  "Subscription Purchases" must {

    val configuration = Configuration.from(
      Map(
        "google.packageName" -> "somePackageName",
        "google.playDeveloperRefreshToken" -> "someRefreshToken",
        "swg.clientId" -> "someClientId",
        "swg.clientSecret" -> "someClientSecret",
        "swg.redirectUri" -> "someRedirectUri",
        "google.api-endpoint" -> "https://www.googleapis.com",
        "google.account-endpoint" -> "https://www.account.google.com"
      )
    )

    val timeout = Timeout(Span(500, Millis))
    val interval = Interval(Span(25, Millis))

    "Retrieve and deserialise a subscription purchase" in {
      val ws = MockWS {
        case (
            GET,
            "https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/purchases/subscriptions/someProductId/tokens/somePurchaseToken"
            ) =>
          Action {
            Ok(s"""{
                  |"kind": "androidpublisher#subscriptionPurchase",
                  |"startTimeMillis": "1",
                  |"expiryTimeMillis": "1",
                  |"autoRenewing": true,
                  |"priceCurrencyCode": "GBP",
                  |"priceAmountMicros": "1990000",
                  |"countryCode": "GB",
                  |"developerPayload": "devPayload",
                  |"paymentState": 1,
                  |"cancelReason": 1,
                  |"userCancellationTimeMillis": "1",
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

      val googleHttpClient =
        new GoogleHTTPClient(ws, mockAccessTokenClient, configuration)

      val eventualPurchase = googleHttpClient
      .getSubscriptionPurchase(SKUCode("someProductId"), "somePurchaseToken")

      whenReady(
        eventualPurchase,
        timeout,
        interval
      ) { result =>
        result shouldBe SubscriptionPurchase(
          "androidpublisher#subscriptionPurchase",
          "1",
          "1",
          Some(true),
          "GBP",
          "1990000",
          "GB",
          "devPayload",
          1,
          Some(1),
          Some("1"),
          Some(CancelSurveyResult(1, "reason")),
          "orderId",
          Some("linkedPurchaseToken"),
          Some(1),
          Some("profileName"),
          Some("email"),
          Some("givenName"),
          Some("familyName"),
          Some("profileId"),
        )
      }

      whenReady(
        eventualPurchase,
        timeout,
        interval
      )( result => result.priceAmount shouldBe BigDecimal(1.99))
    }

    "Fail with invalid JSON" in {
      val ws = MockWS {
        case (
            GET,
            "https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/purchases/subscriptions/someProductId/tokens/somePurchaseToken"
            ) =>
          Action {
            Ok(s"""{"packageName": "packageName"}""")
          }
      }

      val googleHttpClient =
        new GoogleHTTPClient(ws, mockAccessTokenClient, configuration)

      whenReady(
        googleHttpClient
          .getSubscriptionPurchase(SKUCode("someProductId"), "somePurchaseToken") failed,
        timeout,
        interval
      ) { result =>
        result shouldBe a[GoogleHTTPClientDeserializationException]
      }
    }

    "Fail if HTTP request fails" in {
      val ws = MockWS {
        case (
            GET,
            "https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/purchases/subscriptions/someProductId/tokens/somePurchaseToken"
            ) =>
          Action {
            InternalServerError("Some Server Error")
          }
      }

      val subPurchaseLookup =
        new GoogleHTTPClient(ws, mockAccessTokenClient, configuration)

      whenReady(
        subPurchaseLookup
          .getSubscriptionPurchase(SKUCode("someProductId"), "somePurchaseToken") failed
      ) { result =>
        result shouldBe GoogleHTTPClientException(
          INTERNAL_SERVER_ERROR,
          "Attempt to connect to https://www.googleapis.com/androidpublisher/v3/applications/somePackageName/purchases/subscriptions/someProductId/tokens/somePurchaseToken - Received status 500"
        )
      }
    }
  }
}
