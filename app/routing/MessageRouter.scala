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
import scala.util.Try

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
      developerNotification <- parsePushMessageBody[DeveloperNotification](wrapper.message.decodedData)
      subscriptionDeveloperNotification <- convertToSubscriptionDeveloperNotification(developerNotification)
      contributionWithType <- supportedSku(subscriptionDeveloperNotification)
      supportedNotificationType <- checkNotificationType(contributionWithType)
      enrichedContribution <- enrichWithSubscriptionPurchase(supportedNotificationType)
      paymentRecord <- createPaymentRecord(enrichedContribution)
      paymentRequest <- sendRequestToPaymentAPI(paymentRecord)
    } yield paymentRequest

    exceptionOrNotification.value
  }

  private def convertToSubscriptionDeveloperNotification(developerNotification: DeveloperNotification)
    : EitherT[Future, IgnoreTestNotificationException, SubscriptionDeveloperNotification] = {
    EitherT.fromEither[Future](developerNotification match {
      case sdn: SubscriptionDeveloperNotification => Right(sdn)
      case test: TestDeveloperNotification =>
        monitoringService.addReceivedTestNotification()
        Left(IgnoreTestNotificationException("Received Test notification - Ignoring"))
    })
  }

  private def supportedSku(subscriptionDeveloperNotification: SubscriptionDeveloperNotification)
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
  private def checkNotificationType(contribution: Contribution): EitherT[Future, Exception, Contribution] = {
    EitherT.fromEither[Future](
      contribution.subscriptionDeveloperNotification.subscriptionNotification.notificationType match {
        case NotificationType.SubscriptionPurchased => Right(contribution)
        case _ =>
          monitoringService.addUnsupportedNotificationType()
          Left(UnsupportedNotificationTypeException("This notification type is not supported"))
      })
  }

  private def sendRequestToPaymentAPI(paymentRecord: PaymentRecord): EitherT[Future, Exception, Unit] = {
    val paymentResult = paymentRecord.status match {
      case Paid     => paymentClient.createPaymentRecord(paymentRecord)
      case Refunded => paymentClient.refundPaymentRecord(paymentRecord)
    }

    EitherT(
      paymentResult
        .map{ res =>
          logger.info("Successfully sent to payment-api")
          monitoringService.addSendSuccessful()
          Right(res)
        }
        .recover {
          case e: Exception => {
            //todo: Check how compliant this is - logging out customer emails???
            logger.error(s"Failure to send to payment-api :: payment-record: $paymentRecord")
            monitoringService.addSendFailure()
            Left(e)
          }
        })
  }

  private def enrichWithSubscriptionPurchase(
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

  private def createPaymentRecord(contributionWithSubscriptionPurchase: ContributionWithSubscriptionPurchase)
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

  private def createSingleContributionPaymentRecord(contributionWithSubscriptionPurchase: ContributionWithSubscriptionPurchase)
    : Either[UnsupportedNotificationTypeException, PaymentRecord] = {
    (contributionWithSubscriptionPurchase.subscriptionDeveloperNotification.subscriptionNotification.notificationType,
     contributionWithSubscriptionPurchase.subscriptionPurchase.emailAddress) match {
      case (NotificationType.SubscriptionPurchased, Some(email)) =>
        val purchaseData = contributionWithSubscriptionPurchase.subscriptionPurchase
        Right(
          PaymentRecord(
            purchaseData.customerNameOpt.getOrElse(""),
            email,
            Paid,
            purchaseData.priceAmount,
            purchaseData.priceCurrencyCode,
            purchaseData.countryCode,
            purchaseData.orderId,
            System.currentTimeMillis()
          )
        )
      case a =>
        Left(UnsupportedNotificationTypeException(s"Unable to create to create payment record for ${a.toString}"))
    }
  }

  private def parsePushMessageBody[A: Reads](jsonStr: String): EitherT[Future, Exception, A] = {

    def validateJson[A: Reads](json: JsValue): Either[DeserializationException, A] = {
      json.validate[A] match {
        case JsSuccess(value, _) => Right(value)
        case JsError(errors) =>
          monitoringService.addDeserializationFailure()
          logger.error(s"Failure to deserialize push request from pub sub :: $errors")
          Left(DeserializationException("Failure to deserialize push request from pub sub", errors))
      }
    }

    val attemptedParse = Try(Json.parse(jsonStr)).toEither.leftMap(l => {
        val exception = DeserializationException(
          s"Failure to deserialize push request from pub sub : Received Notification containing $jsonStr")
        logger.error(exception.message, l)
        exception
      })

    val validatedJson = for {
      jsonVal <- attemptedParse
      validatedJsVal <- validateJson(jsonVal)
    } yield validatedJsVal

    EitherT.fromEither[Future](validatedJson)

  }
}
