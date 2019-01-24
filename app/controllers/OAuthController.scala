package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

@Singleton
class OAuthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  private val MaxViews = 4

  def oauth(count: Int = 0) = Action { implicit request: Request[AnyContent] =>
    if (count >= MaxViews) {
      ???
    } else {
      ???
    }
  }




}
