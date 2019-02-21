package exceptions

import play.api.libs.json.{JsPath, JsonValidationError}

case class GoogleHTTPClientDeserializationException(message: String, errors: Seq[(JsPath, Seq[JsonValidationError])])
    extends Exception(message)
