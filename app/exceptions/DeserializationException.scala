package exceptions

import play.api.libs.json.{JsPath, JsonValidationError}

case class DeserializationException(message: String, errors: Seq[(JsPath, Seq[JsonValidationError])])
  extends Exception(message) {
}


object DeserializationException {
  def apply(message: String): DeserializationException = {
    new DeserializationException(message, Seq.empty)
  }

  def apply(message: String, errors: Seq[(JsPath, Seq[JsonValidationError])]): DeserializationException = {
    new DeserializationException(s"$message with Json Errors: $errors", errors)
  }
}
