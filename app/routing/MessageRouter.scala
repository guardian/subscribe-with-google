package routing

import cats.data.EitherT
import cats.implicits._
import exceptions.{DeserializationException, IgnoreTestNotificationException}
import model.PaymentStatus.Paid
import model.{SubscriptionDeveloperNotification, _}
import play.api.Logger._
import play.api.libs.json._
import services.{GoogleHTTPClient, PaymentHTTPClient, SKUClient}

import scala.concurrent.{ExecutionContext, Future}

sealed trait Contribution {
  val subscriptionDeveloperNotification: SubscriptionDeveloperNotification
}
case class RecurringContribution(subscriptionDeveloperNotification: SubscriptionDeveloperNotification)
    extends Contribution
case class SingleContribution(subscriptionDeveloperNotification: SubscriptionDeveloperNotification) extends Contribution

class MessageRouter(googleHTTPClient: GoogleHTTPClient, paymentClient: PaymentHTTPClient, skuClient: SKUClient)(
    implicit ec: ExecutionContext) {

  def handleMessage(
      message: () => Either[Exception, GooglePushMessageWrapper]): Future[Either[Exception, Contribution]] = {
    val googlePushMessageWrapper = message.apply()
    //todo: Handle all messages here
    val exceptionOrNotification = for {
      wrapper <- EitherT.fromEither[Future](googlePushMessageWrapper)
      developerNotification <- EitherT.fromEither[Future](
        parsePushMessageBody[DeveloperNotification](Json.parse(wrapper.message.decodedData)))
      subscriptionDeveloperNotification <- EitherT.fromEither[Future](
        convertToSubscriptionDeveloperNotification(developerNotification))
      contributionWithType <- EitherT(supportedSku(subscriptionDeveloperNotification))
    } yield contributionWithType

    exceptionOrNotification.value
  }

  def convertToSubscriptionDeveloperNotification(developerNotification: DeveloperNotification)
    : Either[IgnoreTestNotificationException, SubscriptionDeveloperNotification] = {
    developerNotification match {
      case sdn: SubscriptionDeveloperNotification => Right(sdn)
      case test: TestDeveloperNotification =>
        Left(IgnoreTestNotificationException("Received Test notification - Ignoring"))
      //cloudwatch stat out
    }
  }

  def supportedSku(
      subscriptionDeveloperNotification: SubscriptionDeveloperNotification): Future[Either[Exception, Contribution]] = {
    EitherT(skuClient.getSkuType(subscriptionDeveloperNotification.subscriptionNotification.subscriptionId))
      .bimap(
        e => e, {
          case SKUType.Recurring => RecurringContribution(subscriptionDeveloperNotification)
          case SKUType.Single    => SingleContribution(subscriptionDeveloperNotification)
        }
      )
      .value
  }

  //todo: test notification types for refund
  def routeNotification(subscriptionDeveloperNotification: SubscriptionDeveloperNotification)
    : Either[Unit, SubscriptionDeveloperNotification] = {
    subscriptionDeveloperNotification.subscriptionNotification.notificationType match {
      case NotificationType.SubscriptionPurchased => Right(subscriptionDeveloperNotification)
      case _                                      => Left()
    }
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

  private def parsePushMessageBody[A: Reads](json: JsValue): Either[Exception, A] = {
    json.validate[A] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) =>
        Left(DeserializationException("Failure to deserialize push request from pub sub", errors))
    }
  }
}
