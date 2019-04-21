package services
import akka.stream.alpakka.sqs.SqsPublishResult
import cats.data.EitherT
import javax.inject.{Inject, Singleton}
import model.email.ContributorRow
import queue.EmailSQSQueue

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WelcomeEmailService @Inject()(emailSQSQueue: EmailSQSQueue) {

  def sendEmail(contributorRow: ContributorRow)(
      implicit ec: ExecutionContext): EitherT[Future, Exception, SqsPublishResult] = {
    EitherT(emailSQSQueue.sendMessage(contributorRow).map(Right.apply).recover {
      case e: Exception => Left(new Exception(s"Failed to send message for : $contributorRow"))
    })
  }

}
