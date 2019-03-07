package routing

import java.util.Base64
import java.util.concurrent.TimeoutException

import exceptions._
import model.PaymentStatus.Paid
import model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import services.{GoogleHTTPClient, MonitoringService, PaymentHTTPClient, SKUClient}
import org.mockito.Mockito._
import org.mockito.{Matchers => Match}

import scala.concurrent.{ExecutionContext, Future}

class MessageRouterFixture extends MockitoSugar {
  implicit val ec = ExecutionContext.global

  val mockGoogleHttpClient = mock[GoogleHTTPClient]
  val mockPaymentApiClient = mock[PaymentHTTPClient]
  val mockSkuClient = mock[SKUClient]
  val mockMonitoringService = mock[MonitoringService]

  val messageRouter =
    new MessageRouter(mockGoogleHttpClient, mockPaymentApiClient, mockSkuClient, mockMonitoringService)

  val testNotification = new TestNotification("1.0")
  val testDeveloperNotification = TestDeveloperNotification("1.0", "org.gu", 1234l, testNotification)

  val testNotificationPushMessage = GooglePushMessage(None,
                                                      new String(
                                                        Base64.getEncoder.encode(
                                                          Json
                                                            .asciiStringify(
                                                              Json.toJson(testDeveloperNotification)
                                                            )
                                                            .getBytes)),
                                                      "messageId",
                                                      "")

  val subscriptionPurchaseNotification =
    SubscriptionNotification("1.0", NotificationType.SubscriptionPurchased, "purchaseToken", "skuID")

  val subscriptionPriceChangeNotification =
    SubscriptionNotification("1.0", NotificationType.SubscriptionPriceChangeConfirmed, "purchaseToken", "skuID")

  val purchaseDeveloperNotificationWithSubscription: DeveloperNotification =
    SubscriptionDeveloperNotification("1.0", "com.gu", 1L, subscriptionPurchaseNotification)

  val priceChangeDeveloperNotificationWithSubscription: DeveloperNotification =
    SubscriptionDeveloperNotification("1.0", "com.gu", 1L, subscriptionPriceChangeNotification)

  val googlePushMessage = GooglePushMessage(None,
                                            new String(
                                              Base64.getEncoder.encode(
                                                Json
                                                  .asciiStringify(
                                                    Json.toJson(purchaseDeveloperNotificationWithSubscription)
                                                  )
                                                  .getBytes)),
                                            "messageId",
                                            "")

  val priceChangeGooglePushMessage = GooglePushMessage(
    None,
    new String(
      Base64.getEncoder.encode(
        Json
          .asciiStringify(
            Json.toJson(priceChangeDeveloperNotificationWithSubscription)
          )
          .getBytes)),
    "messageId",
    "")

  val googlePushMessageWithInvalidBody = GooglePushMessage(None,
                                                           new String(
                                                             Base64.getEncoder.encode(
                                                               Json
                                                                 .asciiStringify(
                                                                   Json.toJson("{}")
                                                                 )
                                                                 .getBytes)),
                                                           "messageId",
                                                           "")

  val googlePushMessageWrapper = GooglePushMessageWrapper(googlePushMessage, "subscription")
  val priceChangeGooglePushMessageWrapper = GooglePushMessageWrapper(priceChangeGooglePushMessage, "subscription")

  val noEmailSubscriptionPurchase = SubscriptionPurchase(
    "mykind",
    1l,
    2d,
    false,
    "GBP",
    5000000D,
    "UK",
    "nopayloadhere",
    1d,
    0d,
    1l,
    CancelSurveyResult(1d, "This is most likely optional"),
    "893248675345",
    "nolinkedpurchasetoken",
    1d,
    "openprofile",
    None,
    "Optional",
    "Optional",
    "OptionalProfileId"
  )

  val subscriptionPurchase = SubscriptionPurchase(
    "mykind",
    1l,
    2d,
    false,
    "GBP",
    5000000D,
    "UK",
    "nopayloadhere",
    1d,
    0d,
    1l,
    CancelSurveyResult(1d, "This is most likely optional"),
    "893248675345",
    "nolinkedpurchasetoken",
    1d,
    "openprofile",
    Some("guardian@guardian.com"),
    "Optional",
    "Optional",
    "OptionalProfileId"
  )

  val paymentRecord = PaymentRecord(
    "Optional",
    "guardian@guardian.com",
    Paid,
    BigDecimal("5000000"),
    "GBP",
    "UK",
    PaymentRecord.generatePaymentId(subscriptionPurchase),
    System.currentTimeMillis()
  )

}

class MessageRouterSpec extends WordSpecLike with Matchers with MockitoSugar with ScalaFutures {

  "Message router" must {
    "stop processing when message is not deserializable" in {
      val fixture = new MessageRouterFixture()

      val testWrapper: Either[Exception, GooglePushMessageWrapper] =
        Left(DeserializationException("Failure to deserialize ???"))

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      result.futureValue.left.get shouldBe DeserializationException("Failure to deserialize ???")
    }

    "ignore test notifications" in {
      val fixture = new MessageRouterFixture()

      val testWrapper: Either[Exception, GooglePushMessageWrapper] =
        Right(GooglePushMessageWrapper(fixture.testNotificationPushMessage, "subId"))

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      result.futureValue.left.get shouldBe IgnoreTestNotificationException("Received Test notification - Ignoring")
    }

    "discard a non-supported sku" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Left(UnsupportedSKUException("SKU UNSUPPORTEDSKU is not a supported SKU"))))

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      result.futureValue.left.get shouldBe UnsupportedSKUException("SKU UNSUPPORTEDSKU is not a supported SKU")
    }

    "discard a non-payment notification" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Right(SKUType.Single)))

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.priceChangeGooglePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      result.futureValue.left.get shouldBe UnsupportedNotificationTypeException(
        "This notification type is not supported")
    }

    "fail on a timeout for subscription purchase" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Right(SKUType.Single)))

      when(fixture.mockGoogleHttpClient.getSubscriptionPurchase(Match.any(), Match.any()))
        .thenReturn(Future.failed(new TimeoutException("Futures timed out")))

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      val value = result.futureValue.left.get
      value.getMessage shouldEqual new TimeoutException("Futures timed out").getMessage
      value shouldBe a[TimeoutException]
    }

    "return a not implemented exception for single contribution purchases without an email" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Right(SKUType.Single)))

      when(fixture.mockGoogleHttpClient.getSubscriptionPurchase(Match.any(), Match.any()))
        .thenReturn(Future.successful(fixture.noEmailSubscriptionPurchase))

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      val value = result.futureValue.left.get
      value shouldBe UnsupportedOffPlatformPurchaseException(
        "Currently we do not support contributions without email addresses")
    }

    "return a not implemented exception for recurring contribution purchases without an email" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Right(SKUType.Recurring)))

      when(fixture.mockGoogleHttpClient.getSubscriptionPurchase(Match.any(), Match.any()))
        .thenReturn(Future.successful(fixture.noEmailSubscriptionPurchase))

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      val value = result.futureValue.left.get
      value shouldBe UnsupportedOffPlatformPurchaseException(
        "Currently we do not support recurring contributions without email addresses")
    }

    "return a not implemented exception for recurring contribution purchases" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Right(SKUType.Recurring)))

      when(fixture.mockGoogleHttpClient.getSubscriptionPurchase(Match.any(), Match.any()))
        .thenReturn(Future.successful(fixture.subscriptionPurchase))

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      val value = result.futureValue.left.get
      value.getMessage shouldBe "Recurring payments are not supported"
      value shouldBe a[Exception]
    }

    "return a payment record for a valid single contribution purchase" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Right(SKUType.Single)))

      when(fixture.mockGoogleHttpClient.getSubscriptionPurchase(Match.any(), Match.any()))
        .thenReturn(Future.successful(fixture.subscriptionPurchase))

      when(fixture.mockPaymentApiClient.createPaymentRecord(Match.any())).thenReturn(Future.successful())

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      val value = result.futureValue.right.get
      value shouldBe ()
    }

    "fail when unable to communiate with paymentapi" in {
      val fixture = new MessageRouterFixture()

      when(fixture.mockSkuClient.getSkuType(Match.any()))
        .thenReturn(Future.successful(Right(SKUType.Single)))

      when(fixture.mockGoogleHttpClient.getSubscriptionPurchase(Match.any(), Match.any()))
        .thenReturn(Future.successful(fixture.subscriptionPurchase))

      when(fixture.mockPaymentApiClient.createPaymentRecord(Match.any()))
        .thenReturn(Future.failed(new TimeoutException("Futures timed out")))

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(fixture.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      val value = result.futureValue.left.get
      value.getMessage shouldEqual new TimeoutException("Futures timed out").getMessage
      value shouldBe a[TimeoutException]
    }
  }
}
