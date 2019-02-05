package services
import exceptions.{DeserializationException, GoogleHTTPClientDeserialisationException, GoogleHTTPClientException}
import javax.inject.{Inject, Singleton}
import model.GoogleAccessToken
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

trait AccessTokenClient {
  def get(): Future[Either[Exception, String]]
}

@Singleton
class GoogleAccessTokenClient @Inject()(
  wsClient: WSClient,
  config: Configuration
)(implicit executionContext: ExecutionContext)
    extends AccessTokenClient {
  val apiTokenUrl = "https://accounts.google.com/o/oauth2/token"

  val refreshToken = config.get[String]("google.playDeveloperRefreshToken")
  val swgClientId = config.get[String]("swg.clientId")
  val swgClientSecret = config.get[String]("swg.clientSecret")
  val swgRedirectUri = config.get[String]("swg.redirectUri")

  def get(): Future[Either[Exception, String]] = {
    val params = Seq(
      "grant_type" -> "refresh_token",
      "refresh_token" -> refreshToken,
      "client_id" -> swgClientId,
      "client_secret" -> swgClientSecret,
      "redirect_uri" -> swgRedirectUri
    )

    wsClient
      .url(apiTokenUrl)
      .withQueryStringParameters(params: _*)
      .get() map { response =>
      {
        if (response.status != Status.OK) {
          Left(GoogleHTTPClientException(response.status, "Server error"))
        } else {
          Json.parse(response.body).validate[GoogleAccessToken].asEither match {
            case Left(l) =>
              Left(
                DeserializationException(
                  "Error deserialising Google access token JSON",
                  l
                )
              )
            case Right(r) => Right(r.accessToken)
          }
        }
      }
    }
  }
}
