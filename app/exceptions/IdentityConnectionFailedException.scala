package exceptions

case class IdentityConnectionFailedException(e: Throwable) extends Exception(e)
