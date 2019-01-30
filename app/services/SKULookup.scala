package services

import javax.inject._
import play.api.Configuration
import play.api.libs.json.{JsResultException, JsValue}
import play.api.libs.ws._
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import model._

trait HTTPClient

@Singleton
class SKULookup @Inject() (wsClient: WSClient, config: Configuration)(
  implicit executionContext: ExecutionContext
) extends HTTPClient {
  val logger: Logger = Logger(this.getClass())

  def get(sku: String): Future[Either[String, SKU]] = {
    val skuBaseUrl = config.get[String]("google.apiUrl")
    val accessToken = config.get[String]("google.playDeveloperAccessToken")

    val packageName = "com.theguardian.com"
    val url = s"$skuBaseUrl/$packageName/inappproducts/$sku"

    getRequest(wsClient, url, accessToken)
  }

  def getRequest(wsClient: WSClient, url: String, accessToken: String): Future[Either[String, SKU]] =
    wsClient
      .url(url).addHttpHeaders("Authorization" -> accessToken)
      .get() map { response ⇒
        if (response.status >= 400) {
          Left(response.body)
        } else {
          Right(response.body[JsValue].as[SKU])
        }
      } recover {
        case exception: JsResultException => {
          val errorTitle = "Error deserialising SKU"
          val errorMessage = exception.errors.map(err => s"field: ${err._1}, errors: ${err._2}.").mkString(" ")

          logger.error(s"$errorTitle. $errorMessage")
          Left(errorTitle)
        }
        case error: Exception => Left(error.toString)
      }
}
