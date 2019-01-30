package services

import mockws.MockWS
import model._
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._
import play.api.Configuration
import play.api.mvc.Action
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class SKULookupSpec extends WordSpecLike with Matchers with ScalaFutures {
  "SKULookup" must {

    val configuration = Configuration.from(
      Map(
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
            "http://someMockUrl/com.theguardian.com/inappproducts/skuCode"
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

      val skuLookup = new SKULookup(ws, configuration)

      whenReady(skuLookup.get("skuCode"), timeout, interval) { result =>
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
            "http://someMockUrl/com.theguardian.com/inappproducts/skuCode"
            ) =>
          Action {
            Ok(s"""{"packageName": "packageName"}""")
          }
      }

      val skuLookup = new SKULookup(ws, configuration)

      whenReady(skuLookup.get("skuCode"), timeout, interval) { result =>
        result.left.get shouldBe a[SKULookupDeserialisationError]
      }
    }

    "Fail if HTTP request fails" in {
      val ws = MockWS {
        case (
            GET,
            "http://someMockUrl/com.theguardian.com/inappproducts/skuCode"
            ) =>
          Action {
            InternalServerError("Some Server Error")
          }
      }

      val skuLookup = new SKULookup(ws, configuration)

      whenReady(skuLookup.get("skuCode")) { result =>
        result.left.get shouldBe SKULookupError(
          INTERNAL_SERVER_ERROR,
          "Server error"
        )
      }
    }
  }
}
