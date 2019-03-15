package services

import exceptions.{GoogleHTTPClientDeserializationException, GoogleHTTPClientException}
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws._
import play.api.http.Status
import model.{SKU, SKUCode, SubscriptionPurchase}

@Singleton
class GoogleHTTPClient @Inject()(
    wsClient: WSClient,
    accessTokenClient: AccessTokenClient,
    config: Configuration
)(implicit executionContext: ExecutionContext)
    extends HTTPClient {

  val packageName = config.get[String]("google.packageName")
  val googleApiEndpoint = config.get[String]("google.api-endpoint")

  val apiBaseUrl = s"$googleApiEndpoint/androidpublisher/v3/applications"

  def getSKU(sku: SKUCode): Future[SKU] =
    getRequest[SKU](wsClient, s"inappproducts/${sku.sku}")

  def getSubscriptionPurchase(
      sku: SKUCode,
      purchaseToken: String
  ): Future[SubscriptionPurchase] =
    getRequest[SubscriptionPurchase](
      wsClient,
      s"purchases/subscriptions/${sku.sku}/tokens/$purchaseToken"
    )

  def getRequest[A: Reads](wsClient: WSClient, relativeUrl: String): Future[A] = {
    val url = s"$apiBaseUrl/$packageName/$relativeUrl"

    accessTokenClient.get() flatMap { accessToken =>
      getRequestWithAccessToken[A](wsClient, url, accessToken.accessToken)
    }
  }

  def getRequestWithAccessToken[A: Reads](wsClient: WSClient, url: String, accessToken: String): Future[A] = {
    wsClient
      .url(url)
      .addHttpHeaders("Authorization" -> s"Bearer $accessToken")
      .get() map { response =>
      {
        if (response.status != Status.OK) {
          throw GoogleHTTPClientException(response.status, s"Attempt to connect to $url - Received status ${response.status}")
        } else {
          Json.parse(response.body).validate[A].asEither match {
            case Left(l) =>
              throw GoogleHTTPClientDeserializationException(
                "Error deserialising JSON",
                l
              )
            case Right(r) => r
          }
        }
      }
    }
  }
}
