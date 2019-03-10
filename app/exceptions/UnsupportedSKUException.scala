package exceptions

case class UnsupportedSKUException(message: String) extends Exception(message: String) with IgnorableException
