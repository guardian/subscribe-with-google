package controllers
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Helpers.{status, stubControllerComponents}
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._

class AuthorizationControllerSpec extends WordSpecLike with Matchers with GuiceOneAppPerTest with Injecting{

  "Authorization Controller" must {
    "return json" in {
      val controller = new AuthorizationController(stubControllerComponents())

      val action = controller.entitlements().apply(FakeRequest("GET", "/entitlements"))


      status(action) shouldBe OK
      contentType(action).get shouldBe "application/json"
    }
  }


}
