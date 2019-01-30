package services

import javax.inject._
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Logger
import play.api.http.Status

import scala.concurrent.{ExecutionContext, Future}
import model._

trait HTTPClient

case class SKULookupError(status: Int, message: String) extends Exception

case class SKULookupDeserialisationError(
  message: String,
  errors: Seq[(JsPath, Seq[JsonValidationError])]
) extends Exception

@Singleton
class SKULookup @Inject()(wsClient: WSClient, config: Configuration)(
  implicit executionContext: ExecutionContext
) extends HTTPClient {
  val logger: Logger = Logger(this.getClass())
  val apiBaseUrl = config.get[String]("google.apiUrl")
  val accessToken = config.get[String]("google.playDeveloperAccessToken")

  def get(sku: String): Future[Either[Exception, SKU]] = {

    val packageName = "com.theguardian.com"
    val url = s"$apiBaseUrl/$packageName/inappproducts/$sku"

    getRequest(wsClient, url, accessToken)
  }

  def getRequest(wsClient: WSClient,
                 url: String,
                 accessToken: String): Future[Either[Exception, SKU]] =
    wsClient
      .url(url)
      .addHttpHeaders("Authorization" -> accessToken)
      .get() map { response => {
        if (response.status != Status.OK) {
          Left(SKULookupError(response.status, "Server error"))
        } else {
          Json.parse(response.body).validate[SKU].asEither match {
            case Left(l) =>
              Left(SKULookupDeserialisationError("Error deserialising JSON", l))
            case Right(r) => Right(r)
          }
        }
      }
    }
}
