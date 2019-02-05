package controllers

import java.nio.channels.NonReadableChannelException

import exceptions.{DeserializationException, NonJsonBodyException}
import javax.inject.{Inject, Singleton}
import model.{DeveloperNotification, GooglePushMessageWrapper}
import play.api.libs.json._
import play.api.mvc._
import play.api.Logger._
import utils.FlattenableEither._


@Singleton
class PushHandlerController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def receivePush(): Action[AnyContent] = Action { implicit request =>
    val res = parsePushMessageBody[GooglePushMessageWrapper](request.body)
      .map(r => resultToEither(Json.parse(r.message.decodedData).validate[DeveloperNotification])).flatten

    res match {
      case Left(l: NonJsonBodyException) =>
        logger.error("Unable to handle push message", l)
        BadRequest
      case Left(l) =>
        logger.error(s"Failure to deserialise to developer notification: ${request.body.toString}", l)
        NoContent //to guard against spamming failures back for now
      case Right(r) =>
        logger.info(s"Received $r")
        NoContent
    }
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
