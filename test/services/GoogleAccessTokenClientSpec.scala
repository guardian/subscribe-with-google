package services

import exceptions.{DeserializationException, GoogleHTTPClientException}
import mockws.{MockWS, Route}
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.test.Helpers._
import utils.MockWSHelper

import scala.concurrent.ExecutionContext.Implicits.global

class GoogleAccessTokenClientSpec
    extends WordSpecLike
    with Matchers
    with ScalaFutures
    with MockWSHelper {
  "Access Token" must {
    val configuration = Configuration.from(
      Map(
        "google.packageName" -> "somePackageName",
        "google.playDeveloperRefreshToken" -> "someRefreshToken",
        "swg.clientId" -> "someClientId",
        "swg.clientSecret" -> "someClientSecret",
        "swg.redirectUri" -> "someRedirectUri",
        "google.account-endpoint" -> "https://accounts.google.com",
        "google.api-endpoint" -> "https://www.googleapis.com"

      )
    )

    val timeout = Timeout(Span(2000, Millis))
    val interval = Interval(Span(25, Millis))

    "Retrieve and deserialise an Access Token" in {
      val ws = MockWS {
        case (POST, "https://accounts.google.com/o/oauth2/token") =>
          Action {
            Ok(s"""{
                  |"access_token": "someAccessToken",
                  |"expires_in": 3600,
                  |"scope": "https://www.googleapis.com/auth/androidpublisher",
                  |"token_type": "Bearer"
                  |}""".stripMargin)
          }
      }

      val googleAccessTokenClient =
        new GoogleAccessTokenClient(ws, configuration)

      whenReady(googleAccessTokenClient.get(), timeout, interval) { response =>
        response.accessToken shouldBe "someAccessToken"
      }
    }

    "Subsequent retrieval uses cache" in {
      val route = Route {
        case (POST, "https://accounts.google.com/o/oauth2/token") =>
          Action {
            Ok(s"""{
                  |"access_token": "someAccessToken",
                  |"expires_in": 3600,
                  |"scope": "https://www.googleapis.com/auth/androidpublisher",
                  |"token_type": "Bearer"
                  |}""".stripMargin)
          }
      }

      val ws = MockWS(route)

      val googleAccessTokenClient =
        new GoogleAccessTokenClient(ws, configuration)

      await(googleAccessTokenClient.get())

      await(googleAccessTokenClient.get())

      route.timeCalled shouldBe 1
    }

    "Fail with invalid JSON" in {
      val ws = MockWS {
        case (POST, "https://accounts.google.com/o/oauth2/token") =>
          Action {
            Ok(s"""{
                  |"access_token": "someAccessToken"
                  }""".stripMargin)
          }
      }

      val googleAccessTokenClient =
        new GoogleAccessTokenClient(ws, configuration)

      whenReady(googleAccessTokenClient.get() failed, timeout, interval) {
        response =>
          response shouldBe a[DeserializationException]
      }
    }

    "Fail if HTTP request fails" in {
      val ws = MockWS {
        case (POST, "https://accounts.google.com/o/oauth2/token") =>
          Action {
            InternalServerError("Some Server Error")
          }
      }

      val googleAccessTokenClient =
        new GoogleAccessTokenClient(ws, configuration)

      whenReady(googleAccessTokenClient.get() failed) { result =>
        result shouldBe GoogleHTTPClientException(
          INTERNAL_SERVER_ERROR,
          "Server error"
        )
      }
    }
  }
}
