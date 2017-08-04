package controllers

import cats.data.NonEmptyList
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneId }
import play.api.libs.circe.Circe
import play.api.mvc.{ AbstractController, Action, ControllerComponents, Results }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import io.circe.syntax._
import io.circe.generic.auto._

import javax.inject._

class MonitorAndAlertController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Circe {

  import MonitorAndAlert._

  def push() = Action(circe.json[Event]).async { implicit request =>
    val body = request.body
    println("---------------")
    println(body)
    
//    val event = Event(CreationDate(LocalDateTime.ofInstant(t1, ZoneId.systemDefault())), Failed)

//    monitorAndAlert(Event())

    Future(Results.Created("blah"))
  }

}

object MonitorAndAlert {
  // todo: monitor and alert

  import cats.syntax.either._

  sealed trait Status
  final case object Succeeded extends Status
  final case object Failed extends Status

  case class CreationDate(date: LocalDateTime)
  //case class Event(creationDate: CreationDate, status: Status)

  import io.circe.generic.extras._

  implicit val config: Configuration = Configuration.default.withSnakeCaseKeys // .copy(
//    transformKeys = {
//      case "_id" => "id"
//      case other => other
//    }
//  )

  @ConfiguredJsonCodec case class Event(@JsonKey("_id") id: String, userName: String) // (status: Status)

  type AlertOr[A] = Either[NonEmptyList[String], A]

  val t1 = Instant.ofEpochMilli(1485344457000L)
  val t2 = t1.minusSeconds(30 * 60)


  def monitor(event: Event): AlertOr[Event] = event match {
    case Event(_, userName) => //(creationDate, Succeeded) =>
      event.asRight[NonEmptyList[String]]
//    case Event(creationDate, Failed) =>
//      NonEmptyList.of(event.toString).asLeft[Event]
    case _ => ???
  }

  def alert(alertOr: AlertOr[Event]): AlertOr[Boolean] = alertOr.fold(
    nel => {
      println(nel.head)
      true.asRight[NonEmptyList[String]]
    },
    _ => false.asRight[NonEmptyList[String]]
  )

  val monitorAndAlert = monitor _ andThen alert _

}
