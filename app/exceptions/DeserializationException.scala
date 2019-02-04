package exceptions

import play.api.libs.json.{JsPath, JsonValidationError}

case class DeserializationException(message: String, errors: Seq[(JsPath, Seq[JsonValidationError])])
  extends Exception(message)


object DeserializationException {
  def apply(message: String): DeserializationException = {
    DeserializationException(message, Seq.empty)
  }
}
