package utils

import akka.actor.{ActorSystem, Terminated}
import akka.stream.ActorMaterializer
import play.api.mvc.{DefaultActionBuilder, PlayBodyParsers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * See https://github.com/leanovate/play-mockws/issues/29
  */
trait MockWSHelper {
  private implicit val sys = ActorSystem("test")
  private implicit val mat = ActorMaterializer()

  val BodyParser: PlayBodyParsers = PlayBodyParsers()
  val Action: DefaultActionBuilder = DefaultActionBuilder(BodyParser.anyContent)(mat.executionContext)

  def afterAll: Terminated = {
    mat.shutdown()
    Await.result(sys.terminate(), 10.seconds)
  }
}
