package exceptions

case class UnsupportedOffPlatformPurchaseException(msg: String) extends Exception(msg)
