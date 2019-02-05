package model
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration}

case class GoogleAccessToken(accessToken: String, expiresIn: Int, scope: String, tokenType: String)

object GoogleAccessToken {
  implicit val config = JsonConfiguration(SnakeCase)
  implicit val format = Json.format[GoogleAccessToken]
}
