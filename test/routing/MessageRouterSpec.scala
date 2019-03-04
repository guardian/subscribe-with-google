package routing

import java.util.Base64

import exceptions.{DeserializationException, IgnoreTestNotificationException, UnsupportedSKUException}
import fixtures.TestFixtures
import fixtures.TestFixtures.developerNotificationWithSubscription
import model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import services.{GoogleHTTPClient, PaymentHTTPClient, SKUClient}
import org.mockito.Mockito._
import org.mockito.{Matchers => Match}

import scala.concurrent.{ExecutionContext, Future}

class MessageRouterFixture extends MockitoSugar {
  implicit val ec = ExecutionContext.global

  val mockGoogleHttpClient = mock[GoogleHTTPClient]
  val mockPaymentApiClient = mock[PaymentHTTPClient]
  val mockSkuClient = mock[SKUClient]
  val messageRouter = new MessageRouter(mockGoogleHttpClient, mockPaymentApiClient, mockSkuClient)

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

      val testWrapper: Either[Exception, GooglePushMessageWrapper] = Right(TestFixtures.googlePushMessageWrapper)

      val result = fixture.messageRouter.handleMessage(() => testWrapper)

      result.futureValue.left.get shouldBe UnsupportedSKUException("SKU UNSUPPORTEDSKU is not a supported SKU")
    }

  }

}
