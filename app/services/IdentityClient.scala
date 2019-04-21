package services

import cats.data.EitherT
import cats.syntax.either._
import cats.instances.future._
import cats.syntax.applicativeError._
import exceptions.{DeserializationException, IdentityConnectionFailedException}
import javax.inject.Inject
import model.identity.{CreateGuestAccountRequestBody, GuestRegistrationResponse, UserResponse}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class IdentityClient @Inject()(
    wsClient: WSClient,
    config: Configuration
)(implicit executionContext: ExecutionContext) {

  private val identityAuthToken = config.get[String]("identity.auth-token")
  private val endpoint = config.get[String]("identity.endpoint")

  private val authHeader = "x-gu-id-client-access-token" -> s"Bearer $identityAuthToken"

  def getUser(emai: String):EitherT[Future, Exception, UserResponse] = {
    wsClient
      .url(s"$endpoint/user")
      .withQueryStringParameters("emailAddress" -> emai)
      .addHttpHeaders(authHeader)
      .get()
      .attemptT
      .leftMap{err => IdentityConnectionFailedException(err)}
      .subflatMap(res => deserialize[UserResponse](res.body))
  }

  def createAccount(email: String): EitherT[Future, Exception, GuestRegistrationResponse] = {
    wsClient
      .url(s"$endpoint/guest")
      .addHttpHeaders(authHeader)
      .post(CreateGuestAccountRequestBody(email))
      .attemptT
      .leftMap{err => IdentityConnectionFailedException(err)}
      .subflatMap(res => deserialize[GuestRegistrationResponse](res.body))
  }

  private def deserialize[A: Reads](body: String): Either[Exception, A] = {
    Json.parse(body).validate[A].asEither.leftMap { errs =>
      DeserializationException(s"Failure to deserialise an identity client request with body :: $body", errs)
    }
  }
}
