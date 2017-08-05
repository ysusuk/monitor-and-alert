package controllers

import cats.data.NonEmptyList
import io.circe.Encoder

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
  import cats.syntax.either._

  import io.circe.{ Decoder, HCursor }
  import io.circe.generic.extras._

  sealed trait Status
  final case object Succeeded extends Status
  final case object Failed extends Status

  implicit val statusEncode: Encoder[Status] =
    Encoder.encodeString.contramap[Status] { status =>
      status match {
        case Succeeded => "succeeded"
        case Failed => "failed"
      }
    }

  implicit val statusDecoder: Decoder[Status] =  
    Decoder.decodeString.emap { str =>
      str.toLowerCase match {
        case "succeeded" => Succeeded.asRight[String]
        case "failed" => Failed.asRight[String]
        case _ => "Status".asLeft[Status]
      }
    }

  case class CreationDate(instant: Instant)

  implicit val encodeCreationDate: Encoder[CreationDate] =
    Encoder.encodeString.contramap[CreationDate](_.instant.toString)

  implicit val creationDateDecoder: Decoder[CreationDate] =  
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(Instant.ofEpochMilli(str.toLong))
        .map(CreationDate(_))
        .leftMap(_ => "CreationDate")
    }

  implicit val config: Configuration = Configuration.default.withSnakeCaseKeys

  // todo: use tags or refined types 
  @ConfiguredJsonCodec case class Event(
    @JsonKey("_id") eventId: String,
    @JsonKey("_type") eventType: String,
    userName: String,
    userId: String,
    sourceIp: String,
    browser: String,
    creationDate: CreationDate,
    status: Status
  )

  type AlertOr[A] = Either[NonEmptyList[String], A]

  val t1 = Instant.ofEpochMilli(1485344457000L)
  val t2 = t1.minusSeconds(30 * 60)

  def monitor(event: Event): AlertOr[Event] = event match {
    case Event(_, _, _, _, _, _, _, _) =>
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
