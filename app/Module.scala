import com.google.inject.AbstractModule
import services.{HTTPClient, GoogleHTTPClient}

class Module extends AbstractModule {
  def configure(): Unit = {
      bind(classOf[HTTPClient])
        .to(classOf[GoogleHTTPClient])
    }
}
