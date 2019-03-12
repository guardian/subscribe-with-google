package controllers

import fixtures.TestFixtures
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.libs.ws.WSClient
import play.api.libs.json._

class PushHandlerIntegrationSpec extends PlaySpec with GuiceOneServerPerSuite {

  override def fakeApplication() = new GuiceApplicationBuilder().configure().build()

  "Receive notification and post to payment API" ignore {
    val wsClient = app.injector.instanceOf[WSClient]
    val url = s"http://localhost:$port/push/handle-message"

    val data = Json.toJson(TestFixtures.googlePushMessageWrapper)

    val response = await(wsClient.url(url).post(data))

    response.status mustBe NO_CONTENT
  }
}
