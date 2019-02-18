package services

import exceptions.{DeserializationException, PaymentClientException}
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws._
import play.api.http.Status
import model.PaymentRecord

@Singleton
class PaymentClient @Inject()(
  wsClient: WSClient,
  config: Configuration
)(implicit executionContext: ExecutionContext)
    extends HTTPClient {
  val apiBaseUrl = config.get[String]("guardian.paymentApiBaseUrl")

  def createPaymentRecord(payload: JsValue): Future[PaymentRecord] =
    wsClient
      .url(s"$apiBaseUrl/swg/payment")
      .post(payload) map { response =>
    {
      if (response.status != Status.OK) {
        throw PaymentClientException(response.status, "Server error")
      } else {
        Json.parse(response.body).validate[PaymentRecord].asEither match {
          case Left(l) =>
            throw DeserializationException(
              "Error deserialising JSON",
              l
            )
          case Right(r) => r
        }
      }
    }
  }
}
