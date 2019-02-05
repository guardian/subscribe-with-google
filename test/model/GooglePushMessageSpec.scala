package model

import java.util.Base64

import fixtures.TestFixtures
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.Json

class GooglePushMessageSpec extends WordSpecLike with Matchers {

    "Google push message" must {
      "deserialise correctly" in {
        val base64Str = new String(Base64.getEncoder.encode(
          Json.asciiStringify(Json.toJson(TestFixtures.developerNotificationWithSubscription)).getBytes)
        )

        val jsonStr = s"""{
                        |  "message" : {
                        |    "data" : "$base64Str",
                        |    "messageId" : "messageId",
                        |    "publishTime" : ""
                        |  },
                        |  "subscription" : "subscription"
                        |}""".stripMargin

        val json = Json.fromJson[GooglePushMessageWrapper](Json.parse(jsonStr))

        TestFixtures.googlePushMessageWrapper shouldBe json.get
      }
    }

}
