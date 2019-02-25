package controllers

import fixtures.TestFixtures
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.{ Matchers => Match }
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import services.MonitoringService


class PushHandlerFixture() extends MockitoSugar {
  val mockMetricClient = mock[MonitoringService]

}


class PushHandlerControllerSpec extends WordSpecLike with Matchers with GuiceOneAppPerTest with Injecting with MockitoSugar {

  "Push Handler" must {
    "handle a correctly formed post request with json data" in {
      val fixture = new PushHandlerFixture()

      val controller = new PushHandlerController(stubControllerComponents(), fixture.mockMetricClient)

      val action = controller.receivePush().apply(FakeRequest("POST", "/push/handle-message")
        .withJsonBody(Json.toJson(TestFixtures.googlePushMessageWrapper)))


      status(action) shouldBe NO_CONTENT
    }

    "handle a incorrect notification json" in {
      val fixture = new PushHandlerFixture()
      val controller = new PushHandlerController(stubControllerComponents(), fixture.mockMetricClient)

      val action = controller.receivePush().apply(FakeRequest("POST", "/push/handle-message")
        .withJsonBody(Json.toJson(TestFixtures.googlePushMessageWithInvalidBody)))

      status(action) shouldBe NO_CONTENT
      verify(fixture.mockMetricClient, times(1)).addDeserializationFailure(Match.any())
    }

    "handle an empty request body" in {
      val fixture = new PushHandlerFixture()

      val controller = new PushHandlerController(stubControllerComponents(), fixture.mockMetricClient)

      val action = controller.receivePush().apply(FakeRequest("POST", "/push/handle-message"))

      status(action) shouldBe BAD_REQUEST
    }

  }



}
