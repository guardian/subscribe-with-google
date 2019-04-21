package services

import cats.implicits._
import cats.data.EitherT
import exceptions.DeserializationException
import play.api.Logger._

import scala.concurrent.{ExecutionContext, Future}

class IdentityService(identityClient: IdentityClient) {

  def getOrCreateIdentity(email: String)(implicit ec: ExecutionContext): EitherT[Future, Exception, Long] = {
    for {
      id <- getIdentity(email)
      identityId <- id.fold(createIdentity(email)) { x =>
        EitherT.pure[Future, Exception](x)
      }
    } yield identityId
  }

  private def getIdentity(email: String)(implicit ec: ExecutionContext): EitherT[Future, Exception, Option[Long]] = {
    identityClient.getUser(email).map(res => Option(res.user.id)).recover {
      case e: DeserializationException => Option.empty[Long]
    }
  }

  private def createIdentity(email: String)(implicit ec: ExecutionContext): EitherT[Future, Exception, Long] = {
    identityClient.createAccount(email).map{ res =>
      logger.info(s"guest account created for email address: $email")
      res.identityId
    }
  }

}
