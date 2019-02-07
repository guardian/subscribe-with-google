import com.google.inject.AbstractModule
import services._

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[HTTPClient])
      .to(classOf[GoogleHTTPClient])

    bind(classOf[AccessTokenClient])
      .to(classOf[GoogleAccessTokenClient])

    bind(classOf[ConfigService])
      .to(classOf[ProductService])
  }
}
