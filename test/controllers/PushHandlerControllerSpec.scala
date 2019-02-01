package controllers

import fixtures.TestFixtures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._


class PushHandlerControllerSpec extends WordSpecLike with Matchers with GuiceOneAppPerTest with Injecting {

  "Push Handler" must {
    "handle a correctly formed post request with json data" in {
      val controller = new PushHandlerController(stubControllerComponents())

      val action = controller.receivePush().apply(FakeRequest("POST", "/push/handle-message")
        .withJsonBody(Json.toJson(TestFixtures.googlePushMessageWrapper)))


      status(action) shouldBe NO_CONTENT
    }


  }



}
