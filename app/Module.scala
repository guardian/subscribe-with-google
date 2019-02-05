import com.google.inject.AbstractModule
import services.{AccessTokenClient, GoogleAccessTokenClient, GoogleHTTPClient, HTTPClient}

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[HTTPClient])
      .to(classOf[GoogleHTTPClient])

    bind(classOf[AccessTokenClient])
      .to(classOf[GoogleAccessTokenClient])
    }
}
