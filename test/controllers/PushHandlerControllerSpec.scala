package controllers

import fixtures.TestFixtures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import services.MonitoringService


class PushHandlerControllerSpec extends WordSpecLike with Matchers with GuiceOneAppPerTest with Injecting with MockitoSugar {


  val mockMetricClient = mock[MonitoringService]


  "Push Handler" must {
    "handle a correctly formed post request with json data" in {
      val controller = new PushHandlerController(stubControllerComponents(), mockMetricClient)

      val action = controller.receivePush().apply(FakeRequest("POST", "/push/handle-message")
        .withJsonBody(Json.toJson(TestFixtures.googlePushMessageWrapper)))


      status(action) shouldBe NO_CONTENT
    }

    "handle a incorrect notification json" in {
      val controller = new PushHandlerController(stubControllerComponents(), mockMetricClient)

      val action = controller.receivePush().apply(FakeRequest("POST", "/push/handle-message")
        .withJsonBody(Json.toJson(TestFixtures.googlePushMessageWithInvalidBody)))


      status(action) shouldBe NO_CONTENT
    }

    "handle an empty request body" in {
      val controller = new PushHandlerController(stubControllerComponents(), mockMetricClient)

      val action = controller.receivePush().apply(FakeRequest("POST", "/push/handle-message"))

      status(action) shouldBe BAD_REQUEST
    }

  }



}
