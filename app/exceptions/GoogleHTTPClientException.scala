package exceptions

case class GoogleHTTPClientException(status: Int, message: String) extends Exception(message)
