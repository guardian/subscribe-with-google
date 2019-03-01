package services

import exceptions.PaymentClientException
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws._
import play.api.http.Status
import model.PaymentRecord

@Singleton
class PaymentHTTPClient @Inject()(
    wsClient: WSClient,
    config: Configuration
)(implicit executionContext: ExecutionContext)
    extends HTTPClient {
  private val apiBaseUrl = config.get[String]("guardian.paymentApiBaseUrl")
  private val swgBaseUrl = s"$apiBaseUrl/contribute/one-off/swg"

  def createPaymentRecord(paymentRecord: PaymentRecord): Future[Unit] =
    wsClient
      .url(s"$swgBaseUrl/record-payment")
      .post(Json.stringify(Json.toJson(paymentRecord))) map { response =>
      {
        if (response.status != Status.OK) {
          throw PaymentClientException(response.status, "Server error")
        }
      }
    }

  def refundPaymentRecord(paymentRecord: PaymentRecord): Future[Unit] =
    wsClient
      .url(s"$swgBaseUrl/refund-payment")
      .post(Json.stringify(Json.toJson(paymentRecord))) map { response =>
      {
        if (response.status != Status.OK) {
          throw PaymentClientException(response.status, "Server error")
        }
      }
    }
}
