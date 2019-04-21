package model.identity
import akka.util.ByteString
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, InMemoryBody}

case class PublicFields(displayName: String)

case class CreateGuestAccountRequestBody(primaryEmailAddress: String, publicFields: PublicFields) {}

object PublicFields {
  implicit val formats = Json.format[PublicFields]
}

object CreateGuestAccountRequestBody {
  implicit val formats = Json.format[CreateGuestAccountRequestBody]

  implicit val bodyWriteable: BodyWritable[CreateGuestAccountRequestBody] = {
    BodyWritable[CreateGuestAccountRequestBody](
      transform = body => InMemoryBody(ByteString.fromString(Json.stringify(Json.toJson(body)))),
      contentType = "application/json"
    )
  }

  private def guestDisplayName(email: String) = email.split("@").headOption.getOrElse("Guest User")

  def apply(email: String): CreateGuestAccountRequestBody =
    CreateGuestAccountRequestBody(email, PublicFields(guestDisplayName(email)))
}
