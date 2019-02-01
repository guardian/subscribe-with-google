package controllers

import exceptions.{DeserializationException, NonJsonBodyException}
import javax.inject.{Inject, Singleton}
import model.{DeveloperNotification, GooglePushMessageWrapper}
import play.api.libs.json._
import play.api.mvc._
import play.api.Logger._

@Singleton
class PushHandlerController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def receivePush(): Action[AnyContent] = Action { implicit request =>
    val res = parsePushMessageBody[GooglePushMessageWrapper](request.body)
      .map(r => resultToEither(Json.parse(r.message.decodedData).validate[DeveloperNotification])) match {
      case Left(l) => Left(l)
      case Right(Left(l)) => Left(l)
      case Right(Right(r)) => Right(r)
    } // todo : Probably write a generic flatten for Either[A, Either[A, B] && Either[Either[A, B], B]

    res match {
      case Left(l) => logger.error("Unable to handle push message", l)
      case Right(r) => logger.info(s"Received $r")
    }


    //todo: Confirm this is desired
    NoContent
  }

  private def parsePushMessageBody[A: Reads](body: AnyContent): Either[Exception, A] = {
    body.asJson.map(js => js.validate[A])
      .fold(Left(NonJsonBodyException(body.toString)): Either[Exception, A])(resultToEither)
  }

  private def resultToEither[A](jsResult: JsResult[A]): Either[Exception, A] = {
    jsResult match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) => Left(DeserializationException("Failure to deserialize push request from pub sub", errors))
    }
  }
}
