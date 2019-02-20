package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class SubscribeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def subscribe() = Action { implicit request =>
    Ok("{}")
  }

}
