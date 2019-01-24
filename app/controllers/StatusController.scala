package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._

@Singleton
class StatusController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request =>
    Ok("Service is alive")
  }

  def ping(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok("pong")
  }

  def healthcheck(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok("alive and kicking")
  }

}
