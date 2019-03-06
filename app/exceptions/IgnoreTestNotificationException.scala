package exceptions

case class IgnoreTestNotificationException(msg: String) extends Exception(msg)