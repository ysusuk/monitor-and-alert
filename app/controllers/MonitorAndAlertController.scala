package controllers

import play.api.mvc.{ AbstractController, Action, ControllerComponents, Results }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._

class MonitorAndAlertController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def push() = Action.async { implicit request =>
    val body = request.body.asText
    println("---------------")
    println(body)
    Future(Results.Created("blah"))
  }
}
