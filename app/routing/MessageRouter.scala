package routing

import cats.data.EitherT
import cats.implicits._
import exceptions.{
  DeserializationException,
  IgnoreTestNotificationException,
  UnsupportedNotificationTypeException,
  UnsupportedOffPlatformPurchaseException
}
import model.PaymentStatus.{Paid, Refunded}
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

sealed trait ContributionWithSubscriptionPurchase {
  val subscriptionDeveloperNotification: SubscriptionDeveloperNotification
  val subscriptionPurchase: SubscriptionPurchase
}

case class SingleContributionWithSubscriptionPurchase(
    subscriptionDeveloperNotification: SubscriptionDeveloperNotification,
    subscriptionPurchase: SubscriptionPurchase)
    extends ContributionWithSubscriptionPurchase
case class RecurringContributionWithSubscriptionPurchase(
    subscriptionDeveloperNotification: SubscriptionDeveloperNotification,
    subscriptionPurchase: SubscriptionPurchase)
    extends ContributionWithSubscriptionPurchase

object ContributionWithSubscriptionPurchase {

  def apply(contribution: Contribution,
            subscriptionPurchase: SubscriptionPurchase): ContributionWithSubscriptionPurchase = {
    contribution match {
      case c: SingleContribution =>
        SingleContributionWithSubscriptionPurchase(c.subscriptionDeveloperNotification, subscriptionPurchase)
      case c: RecurringContribution =>
        RecurringContributionWithSubscriptionPurchase(c.subscriptionDeveloperNotification, subscriptionPurchase)
    }
  }
}

class MessageRouter(googleHTTPClient: GoogleHTTPClient, paymentClient: PaymentHTTPClient, skuClient: SKUClient)(
    implicit ec: ExecutionContext) {

  def handleMessage(message: () => Either[Exception, GooglePushMessageWrapper]): Future[Either[Exception, Unit]] = {
    val googlePushMessageWrapper = EitherT.fromEither[Future](message())

    val exceptionOrNotification = for {
      wrapper <- googlePushMessageWrapper
      developerNotification <- parsePushMessageBody[DeveloperNotification](Json.parse(wrapper.message.decodedData))
      subscriptionDeveloperNotification <- convertToSubscriptionDeveloperNotification(developerNotification)
      contributionWithType <- supportedSku(subscriptionDeveloperNotification)
      supportedNotificationType <- checkNotificationType(contributionWithType)
      enrichedContribution <- enrichWithSubscriptionPurchase(supportedNotificationType)
      paymentRecord <- createPaymentRecord(enrichedContribution)
      paymentRequest <- sendRequestToPaymentAPI(paymentRecord)
    } yield paymentRequest

    exceptionOrNotification.value
  }

  def convertToSubscriptionDeveloperNotification(developerNotification: DeveloperNotification)
    : EitherT[Future, IgnoreTestNotificationException, SubscriptionDeveloperNotification] = {
    EitherT.fromEither[Future](developerNotification match {
      case sdn: SubscriptionDeveloperNotification => Right(sdn)
      case test: TestDeveloperNotification =>
        Left(IgnoreTestNotificationException("Received Test notification - Ignoring"))
      //cloudwatch stat out
    })
  }

  def supportedSku(subscriptionDeveloperNotification: SubscriptionDeveloperNotification)
    : EitherT[Future, Exception, Contribution] = {
    EitherT(skuClient.getSkuType(subscriptionDeveloperNotification.subscriptionNotification.subscriptionId))
      .bimap(
        e => e, {
          case SKUType.Recurring => RecurringContribution(subscriptionDeveloperNotification)
          case SKUType.Single    => SingleContribution(subscriptionDeveloperNotification)
        }
      )
  }

  //todo: test notification types for refund
  def checkNotificationType(contribution: Contribution): EitherT[Future, Exception, Contribution] = {
    EitherT.fromEither[Future](
      contribution.subscriptionDeveloperNotification.subscriptionNotification.notificationType match {
        case NotificationType.SubscriptionPurchased => Right(contribution)
        case _ =>
          Left(UnsupportedNotificationTypeException("This notification type is not supported"))
      })
  }

  def sendRequestToPaymentAPI(paymentRecord: PaymentRecord): EitherT[Future, Exception, Unit] = {
    val paymentResult = paymentRecord.status match {
      case Paid     => paymentClient.createPaymentRecord(paymentRecord)
      case Refunded => paymentClient.refundPaymentRecord(paymentRecord)
    }

    EitherT(
      paymentResult
        .map(res => Right(res))
        .recover {
          case e: Exception => Left(e)
        })
  }

  def enrichWithSubscriptionPurchase(
      contribution: Contribution): EitherT[Future, Exception, ContributionWithSubscriptionPurchase] = {
    EitherT(
      googleHTTPClient
        .getSubscriptionPurchase(
          SKUCode(contribution.subscriptionDeveloperNotification.subscriptionNotification.subscriptionId),
          contribution.subscriptionDeveloperNotification.subscriptionNotification.purchaseToken
        )
        .map(subPurchase => Right(ContributionWithSubscriptionPurchase(contribution, subPurchase)))
        .recover { case e: Exception => Left(e) })
  }

  def createPaymentRecord(contributionWithSubscriptionPurchase: ContributionWithSubscriptionPurchase)
    : EitherT[Future, Exception, PaymentRecord] = {
    val paymentRecord = (contributionWithSubscriptionPurchase,
                         contributionWithSubscriptionPurchase.subscriptionPurchase.emailAddress) match {
      case (single: SingleContributionWithSubscriptionPurchase, None) =>
        Left(
          UnsupportedOffPlatformPurchaseException("Currently we do not support contributions without email addresses"))
      case (single: SingleContributionWithSubscriptionPurchase, Some(_)) =>
        createSingleContributionPaymentRecord(single)
      case (recurring: RecurringContributionWithSubscriptionPurchase, None) =>
        Left(
          UnsupportedOffPlatformPurchaseException("Currently we do not support contributions without email addresses"))

      case (recurring: RecurringContributionWithSubscriptionPurchase, Some(_)) =>
        //todo: Better exception required
        Left(new Exception("Recurring payments are not supported"))
    }

    EitherT.fromEither[Future](paymentRecord)
  }

  def createSingleContributionPaymentRecord(contributionWithSubscriptionPurchase: ContributionWithSubscriptionPurchase)
    : Either[UnsupportedNotificationTypeException, PaymentRecord] = {
    (contributionWithSubscriptionPurchase.subscriptionDeveloperNotification.subscriptionNotification.notificationType,
     contributionWithSubscriptionPurchase.subscriptionPurchase.emailAddress) match {
      case (NotificationType.SubscriptionPurchased, Some(email)) =>
        val purchaseData = contributionWithSubscriptionPurchase.subscriptionPurchase
        Right(
          PaymentRecord(
            purchaseData.givenName,
            email,
            Paid,
            purchaseData.priceAmountMicros,
            purchaseData.priceCurrencyCode,
            purchaseData.countryCode,
            PaymentRecord.generatePaymentId(purchaseData),
            System.currentTimeMillis()
          )
        )
      case a =>
        Left(UnsupportedNotificationTypeException(s"Unable to create to create payment record for ${a.toString}"))
    }
  }

  private def parsePushMessageBody[A: Reads](json: JsValue): EitherT[Future, Exception, A] = {
    EitherT.fromEither[Future](json.validate[A] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) =>
        Left(DeserializationException("Failure to deserialize push request from pub sub", errors))
    })
  }
}
