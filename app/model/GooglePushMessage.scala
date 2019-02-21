package model

import java.util.Base64

import play.api.libs.json.{Format, Json}

case class GooglePushMessageWrapper(message: GooglePushMessage, subscription: String)

//todo: Marshal publishTime to LocalDateTime if required later
case class GooglePushMessage(attributes: Option[Map[String, String]],
                             data: String,
                             messageId: String,
                             publishTime: String) {

  val decodedData = new String(Base64.getDecoder.decode(data))
}

object GooglePushMessageWrapper {
  implicit val formats: Format[GooglePushMessageWrapper] = Json.format[GooglePushMessageWrapper]
}

object GooglePushMessage {
  implicit val formats: Format[GooglePushMessage] = Json.format[GooglePushMessage]
}
