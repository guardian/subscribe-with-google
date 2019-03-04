package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._

@Singleton
class AuthorizationController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def entitlements(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(Json.toJson("{}")).as("application/json")
  }
}
