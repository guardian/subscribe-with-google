package services

import com.github.blemale.scaffeine.{AsyncLoadingCache, Scaffeine}
import exceptions.{DeserializationException, GoogleHTTPClientException}
import javax.inject.{Inject, Singleton}
import model.GoogleAccessToken
import scala.concurrent.duration._
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.Logger._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

trait AccessTokenClient {
  def get(): Future[GoogleAccessToken]
}

@Singleton
class GoogleAccessTokenClient @Inject()(
    wsClient: WSClient,
    config: Configuration
)(implicit executionContext: ExecutionContext)
    extends AccessTokenClient {

  private val refreshToken = config.get[String]("google.playDeveloperRefreshToken")
  private val swgClientId = config.get[String]("swg.clientId")
  private val swgClientSecret = config.get[String]("swg.clientSecret")
  private val swgRedirectUri = config.get[String]("swg.redirectUri")

  private val googleAccountEndpoint = config.get[String]("google.account-endpoint")

  private val apiTokenUrl = s"$googleAccountEndpoint/o/oauth2/token"

  private val cache: AsyncLoadingCache[String, GoogleAccessToken] =
    Scaffeine()
      .recordStats()
      .maximumSize(1)
      .expireAfter(
        create = (_: String, accessToken: GoogleAccessToken) => accessToken.expiresIn seconds,
        update = (_: String, _: GoogleAccessToken, currentDuration: Duration) => currentDuration,
        read = (_: String, _: GoogleAccessToken, currentDuration: Duration) => currentDuration
      )
      .buildAsyncFuture[String, GoogleAccessToken](
        (_: String) => getAccessTokenRequest()
      )

  def get(): Future[GoogleAccessToken] = cache.get("accessToken")

  private def getAccessTokenRequest(): Future[GoogleAccessToken] = {
    val params = Map(
      "grant_type" -> "refresh_token",
      "refresh_token" -> refreshToken,
      "client_id" -> swgClientId,
      "client_secret" -> swgClientSecret,
      "redirect_uri" -> swgRedirectUri
    )

    wsClient
      .url(apiTokenUrl)
      .post(params) map { response =>
      {
        if (response.status != Status.OK) {
          logger.error(
            s"Failure to retrieve correct response when calling $apiTokenUrl with params: $params: Response code ${response.status} with body : ${response.body}")
          throw GoogleHTTPClientException(response.status, "Server error")
        } else {
          Json.parse(response.body).validate[GoogleAccessToken].asEither match {
            case Left(l) =>
              throw DeserializationException(
                "Error deserialising Google access token JSON",
                l
              )
            case Right(r) => r
          }
        }
      }
    }
  }
}
