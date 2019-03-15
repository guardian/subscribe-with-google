package exceptions

import play.api.libs.json.{JsPath, JsonValidationError}

case class GoogleHTTPClientDeserializationException(message: String, errors: Seq[(JsPath, Seq[JsonValidationError])])
    extends Exception(message)


object GoogleHTTPClientDeserializationException {
  def apply(message: String): GoogleHTTPClientDeserializationException = {
    new GoogleHTTPClientDeserializationException(message, Seq.empty)
  }

  def apply(message: String, errors: Seq[(JsPath, Seq[JsonValidationError])]): GoogleHTTPClientDeserializationException = {
    new GoogleHTTPClientDeserializationException(s"$message with Json Errors: $errors", errors)
  }

}