package exceptions

case class PaymentClientException(status: Int, message: String) extends Exception(message)
