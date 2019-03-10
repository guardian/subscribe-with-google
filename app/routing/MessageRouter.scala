package routing

import cats.data.EitherT
import cats.implicits._
import exceptions._
import javax.inject.{Inject, Singleton}
import model.PaymentStatus.{Paid, Refunded}
import model.{SubscriptionDeveloperNotification, _}
import play.api.Logger._
import play.api.libs.json._
import routing.adt._
import services.{GoogleHTTPClient, MonitoringService, PaymentHTTPClient, SKUClient}

import scala.concurrent.{ExecutionContext, Future}

trait MessageRouter {
  def handleMessage(message: () => Either[Exception, GooglePushMessageWrapper]): Future[Either[Exception, Unit]]
}

@Singleton
class MessageRouterImpl @Inject()(googleHTTPClient: GoogleHTTPClient,
                    paymentClient: PaymentHTTPClient,
                    skuClient: SKUClient,
                    monitoringService: MonitoringService)(implicit ec: ExecutionContext) extends MessageRouter {

  def handleMessage(message: () => Either[Exception, GooglePushMessageWrapper]): Future[Either[Exception, Unit]] = {
    monitoringService.addMessageReceived()
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

    exceptionOrNotification.value.map{
      case Left(e: IgnorableException) =>
        logger.info(s"Ignorable exception encountered ${e.getMessage}")
        Left(e)
      case Left(e) => Left(e)
      case Right(u) => Right(u)
    }

    exceptionOrNotification.value
  }

  def convertToSubscriptionDeveloperNotification(developerNotification: DeveloperNotification)
    : EitherT[Future, IgnoreTestNotificationException, SubscriptionDeveloperNotification] = {
    EitherT.fromEither[Future](developerNotification match {
      case sdn: SubscriptionDeveloperNotification => Right(sdn)
      case test: TestDeveloperNotification =>
        monitoringService.addReceivedTestNotification()
        Left(IgnoreTestNotificationException("Received Test notification - Ignoring"))
    })
  }

  def supportedSku(subscriptionDeveloperNotification: SubscriptionDeveloperNotification)
    : EitherT[Future, Exception, Contribution] = {
    EitherT(skuClient.getSkuType(subscriptionDeveloperNotification.subscriptionNotification.subscriptionId))
      .bimap(
        e => {
          monitoringService.addUnsupportedSKU()
          e
        }, {
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
          monitoringService.addUnsupportedNotificationType()
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
        .map{ res =>
          monitoringService.addSendSuccessful()
          Right(res)
        }
        .recover {
          case e: Exception => {
            monitoringService.addSendFailure()
            Left(e)
          }
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
        .recover {
          case e: Exception =>
            monitoringService.addFailureToGetSubscriptionPurchase()
            Left(e)
        }
    )
  }

  def createPaymentRecord(contributionWithSubscriptionPurchase: ContributionWithSubscriptionPurchase)
    : EitherT[Future, Exception, PaymentRecord] = {
    val paymentRecord = (contributionWithSubscriptionPurchase,
                         contributionWithSubscriptionPurchase.subscriptionPurchase.emailAddress) match {
      case (single: SingleContributionWithSubscriptionPurchase, None) =>
        monitoringService.addUnsupportedPlatformPurchase()
        Left(
          UnsupportedOffPlatformPurchaseException("Currently we do not support contributions without email addresses"))
      case (single: SingleContributionWithSubscriptionPurchase, Some(_)) =>
        createSingleContributionPaymentRecord(single)
      case (recurring: RecurringContributionWithSubscriptionPurchase, None) =>
        monitoringService.addUnsupportedPlatformPurchase()
        Left(
          UnsupportedOffPlatformPurchaseException("Currently we do not support recurring contributions without email addresses"))

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
        monitoringService.addDeserializationFailure()
        Left(DeserializationException("Failure to deserialize push request from pub sub", errors))
    })
  }
}
