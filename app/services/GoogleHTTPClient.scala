package services

import exceptions.{GoogleHTTPClientDeserialisationException, GoogleHTTPClientException}
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws._
import play.api.http.Status
import model.{SKU, SubscriptionPurchase}

trait HTTPClient

@Singleton
class GoogleHTTPClient @Inject()(wsClient: WSClient, config: Configuration)(
  implicit executionContext: ExecutionContext
) extends HTTPClient {

  val packageName = config.get[String]("google.packageName")
  val apiBaseUrl = config.get[String]("google.apiUrl")
  val accessToken = config.get[String]("google.playDeveloperAccessToken")

  def getSKU(sku: String): Future[Either[Exception, SKU]] = {
    val url = s"$apiBaseUrl/$packageName/inappproducts/$sku"

    getRequest[SKU](wsClient, url, accessToken)
  }

  def getSubscriptionPurchase(
    productId: String,
    purchaseToken: String
  ): Future[Either[Exception, SubscriptionPurchase]] = {
    val url =
      s"$apiBaseUrl/$packageName/purchases/subscriptions/$productId/tokens/$purchaseToken"

    getRequest[SubscriptionPurchase](wsClient, url, accessToken)
  }

  def getRequest[A: Reads](wsClient: WSClient,
                           url: String,
                           accessToken: String): Future[Either[Exception, A]] =
    wsClient
      .url(url)
      .addHttpHeaders("Authorization" -> accessToken)
      .get() map { response => {
        if (response.status != Status.OK) {
          Left(GoogleHTTPClientException(response.status, "Server error"))
        } else {
          Json.parse(response.body).validate[A].asEither match {
            case Left(l) =>
              Left(
                GoogleHTTPClientDeserialisationException(
                  "Error deserialising JSON",
                  l
                )
              )
            case Right(r) => Right(r)
          }
        }
      }
    }
}
