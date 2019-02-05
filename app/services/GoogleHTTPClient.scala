package services

import exceptions.{
  GoogleHTTPClientDeserialisationException,
  GoogleHTTPClientException
}
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws._
import play.api.http.Status
import model.{SKU, SubscriptionPurchase}

trait HTTPClient

@Singleton
class GoogleHTTPClient @Inject()(wsClient: WSClient, accessTokenClient: AccessTokenClient, config: Configuration)(
  implicit executionContext: ExecutionContext
) extends HTTPClient {
  val apiBaseUrl = "https://www.googleapis.com/androidpublisher/v3/applications"

  val packageName = config.get[String]("google.packageName")

  def getSKU(sku: String): Future[Either[Exception, SKU]] =
    getRequest[SKU](wsClient, s"inappproducts/$sku")

  def getSubscriptionPurchase(
    productId: String,
    purchaseToken: String
  ): Future[Either[Exception, SubscriptionPurchase]] =
    getRequest[SubscriptionPurchase](
      wsClient,
      s"purchases/subscriptions/$productId/tokens/$purchaseToken"
    )

  def getRequest[A: Reads](
    wsClient: WSClient,
    relativeUrl: String
  ): Future[Either[Exception, A]] = {
    val url = s"$apiBaseUrl/$packageName/$relativeUrl"

    accessTokenClient.get() flatMap {
      case Right(accessToken: String) =>
        getRequestWithAccessToken[A](wsClient, url, accessToken)
      case Left(l) => Future.successful(Left(l))
    }
  }

  def getRequestWithAccessToken[A: Reads](
    wsClient: WSClient,
    url: String,
    accessToken: String
  ): Future[Either[Exception, A]] = {
    wsClient
      .url(url)
      .addHttpHeaders("Authorization" -> accessToken)
      .get() map { response =>
      {
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
}
