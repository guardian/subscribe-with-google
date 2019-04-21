package model.identity
import play.api.libs.json.Json

case class GuestRegistrationRequest(userId: Long)

case class GuestRegistrationResponse(guestRegistrationRequest: GuestRegistrationRequest) {
  val identityId = guestRegistrationRequest.userId
}

object GuestRegistrationRequest {
  implicit val formats = Json.format[GuestRegistrationRequest]
}

object GuestRegistrationResponse {
  implicit val formats = Json.format[GuestRegistrationResponse]
}
