package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

@Singleton
class OAuthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  private val MaxViews = 4

  def oauth() = Action { implicit request: Request[AnyContent] =>
    Ok(Json.toJson("{}"))
  }
}
