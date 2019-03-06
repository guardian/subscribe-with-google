package exceptions

case class UnsupportedNotificationTypeException(msg: String) extends Exception(msg)
