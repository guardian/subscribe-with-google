package exceptions

import play.api.libs.json.{JsPath, JsonValidationError}

case class GoogleHTTPClientDeserialisationException(message: String,
                                                     errors: Seq[(JsPath, Seq[JsonValidationError])]
                                                   ) extends Exception(message)
