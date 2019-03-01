package routing

import model.PaymentStatus.Paid
import model.{SubscriptionDeveloperNotification, _}
import play.api.Logger._
import services.{GoogleHTTPClient, PaymentHTTPClient}

import scala.concurrent.{ExecutionContext, Future}

class MessageRouter(googleHTTPClient: GoogleHTTPClient, paymentClient: PaymentHTTPClient)(
    implicit ec: ExecutionContext) {

  def handleMessage(message: () => Either[Exception, GooglePushMessageWrapper]): Unit = {
    val googlePushMessageWrapper = message.apply()
    //todo: Handle all messages here
  }

  def handle(developerNotification: DeveloperNotification) = {
    developerNotification match {
      case sdn: SubscriptionDeveloperNotification => ???
      case test: TestDeveloperNotification        => ???
      //cloudwatch stat out
    }
  }

  //todo: test notification types for refund
  def routeNotification(subscriptionDeveloperNotification: SubscriptionDeveloperNotification)
    : Either[Unit, SubscriptionDeveloperNotification] = {
    subscriptionDeveloperNotification.subscriptionNotification.notificationType match {
      case NotificationType.SubscriptionPurchased => Right(subscriptionDeveloperNotification)
      case _                                      => Left()
    }
  }

  def handleSubscriptionType(sdn: SubscriptionDeveloperNotification) = {
    sdn.subscriptionNotification.subscriptionId
  }

  def handlePaymentNotification(subscriptionDeveloperNotification: SubscriptionDeveloperNotification): Future[Unit] = {
    val result = for {
      purchaseData <- googleHTTPClient.getSubscriptionPurchase(
        SKUCode(subscriptionDeveloperNotification.subscriptionNotification.subscriptionId),
        subscriptionDeveloperNotification.subscriptionNotification.purchaseToken
      )
      paymentRecord = PaymentRecord(
        purchaseData.givenName,
        purchaseData.emailAddress,
        Paid,
        purchaseData.priceAmountMicros,
        purchaseData.priceCurrencyCode,
        purchaseData.countryCode,
        PaymentRecord.generatePaymentId(purchaseData),
        System.currentTimeMillis()
      )
      paymentResult <- paymentClient.createPaymentRecord(paymentRecord)
    } yield paymentResult

    result
      .map { _ =>
        logger.info(s"Successful send of one time contribution") //todo: cloudwatch successful send
      }
      .recover {
        case e: Exception =>
          //todo: Cloudwatch stat
          //todo: Retry mechanism - better retry
          logger.error("Failure to track contribution", e)
      }
  }
}
