package controllers
import org.scalatest.{Matchers, TestData, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{status, stubControllerComponents}
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import queue.{SQSListener, SQSListenerImpl}

class AuthorizationControllerSpec extends WordSpecLike with Matchers with GuiceOneAppPerTest with Injecting {

  override def newAppForTest(td: TestData): Application = {
    new GuiceApplicationBuilder().disable[SQSListenerImpl].build()
  }


  "Authorization Controller" must {
    "return json" in {
      val controller = new AuthorizationController(stubControllerComponents())

      val action = controller.entitlements().apply(FakeRequest("GET", "/entitlements"))

      status(action) shouldBe OK
      contentType(action).get shouldBe "application/json"
    }
  }

}
