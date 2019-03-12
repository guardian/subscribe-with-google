package exceptions

case class NonJsonBodyException(message: String) extends Exception(message) with IgnorableException
