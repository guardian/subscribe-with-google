package model.identity
import play.api.libs.json.Json

case class User(id: Long)

case class UserResponse(user: User)

object User {
  implicit val formats = Json.format[User]
}

object UserResponse {
  implicit val formats = Json.format[UserResponse]
}
