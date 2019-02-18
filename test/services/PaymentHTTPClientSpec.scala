package services
import exceptions.{GoogleHTTPClientException, PaymentClientException}
import mockws.MockWS
import model.{PaymentRecord, PaymentStatus}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import play.api.Configuration
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, POST}
import utils.MockWSHelper

import scala.concurrent.ExecutionContext.Implicits.global

class PaymentHTTPClientSpec extends WordSpecLike
  with ScalaFutures
  with Matchers
  with MockWSHelper {
  "Record payment" must {
    val configuration =
      Configuration.from(Map("guardian.paymentApiBaseUrl" -> "paymentUrl"))

    val timeout = Timeout(Span(500, Millis))
    val interval = Interval(Span(25, Millis))
    "success" in {
      val ws = MockWS {
        case (
          POST,
          "paymentUrl"
          ) =>
          Action {
            Ok("{}".stripMargin)
          }
      }
      val client = new PaymentHTTPClient(ws, configuration)
      val paymentRecord = PaymentRecord("firstName", "e-mail", PaymentStatus.Paid, 1.00, "GBP", "UK", "123", 1234)
      whenReady(client.createPaymentRecord(paymentRecord), timeout, interval) {
        _ => succeed
      }
    }

    "server error" in {
      val ws = MockWS {
        case (
          POST,
          "paymentUrl"
          ) =>
          Action {
            InternalServerError("Some Server Error")
          }
      }
      val client = new PaymentHTTPClient(ws, configuration)
      val paymentRecord = PaymentRecord("firstName", "e-mail", PaymentStatus.Paid, 1.00, "GBP", "UK", "123", 1234)
      whenReady(client.createPaymentRecord(paymentRecord) failed, timeout, interval) {
        result => result shouldBe PaymentClientException(
          INTERNAL_SERVER_ERROR,
          "Server error"
        )
      }
    }
  }
}
