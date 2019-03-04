package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.libs.json.JsObject
import play.api.mvc._

@Singleton
class AuthorizationController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def entitlements(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(JsObject.empty).as(ContentTypes.JSON)
  }
}
