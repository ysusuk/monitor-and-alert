package controllers

import cats.data.NonEmptyList
import io.circe.Encoder
import java.time.{ Duration, Instant, LocalDate, LocalDateTime, Period, ZoneId }
import play.api.libs.circe.Circe
import play.api.mvc.{ AbstractController, Action, ControllerComponents, Results }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._

import MonitorAndAlert._

class MonitorAndAlertController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Circe {

  def push() = Action(circe.json[Event]).async { implicit request =>
    val event = request.body
    import EventRepositoryInMemory._

    for {
      _ <- store(event)
      events <- query(event.userName, (start, end))
      _ = println(query(event.userName, (start, end)))
    } yield events

    // 1a store
    // 1b store and then check (get for the user events for the last ... minutes)
    // 2 if check >= 9

    // cache
    // store
    // getBy userName, Period
    Future(Results.Created("blah"))
  }

}

import cats.syntax.either._

trait EventRepository {
  type TimePeriod = (Instant, Instant)
  def store(event: Event): ErrorOr[Event]
  def query(userName: String, timePeriod: TimePeriod): ErrorOr[Seq[Event]]
}

import scala.collection.mutable.{ Map => MMap }

object EventRepositoryInMemory extends EventRepository {
  lazy val repo = MMap.empty[String, Seq[Event]]

  // store is optimized for use case of retriving data by user name and then filter by period
  def store(event: Event): ErrorOr[Event] = {
    repo += (event.userName -> (event +: repo.getOrElse(event.userName, Seq.empty[Event])))
    event.asRight[NonEmptyList[String]]
  }

  def query(userName: String, timePeriod: TimePeriod): ErrorOr[Seq[Event]] = {
    repo.getOrElse(userName, Seq.empty[Event]).asRight[NonEmptyList[String]]
  }
}

object MonitorAndAlert {


  import io.circe.{ Decoder, HCursor }
  import io.circe.generic.auto._
  import io.circe.generic.extras._
  import io.circe.syntax._

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

  type ErrorOr[A] = Either[NonEmptyList[String], A]

  val end = Instant.ofEpochMilli(1485344457000L)
  val start = end.minus(Duration.ofMillis(30 * 60 * 1000L))
  /// instant.isBefore(end) && end.isAfter(start) ?? Equal

  def monitor(event: Event): ErrorOr[Event] = event match {
    case Event(_, _, _, _, _, _, _, _) =>
      event.asRight[NonEmptyList[String]]
//    case Event(creationDate, Failed) =>
//      NonEmptyList.of(event.toString).asLeft[Event]
    case _ => ???
  }

  def alert(alertOr: ErrorOr[Event]): ErrorOr[Boolean] = alertOr.fold(
    nel => {
      true.asRight[NonEmptyList[String]]
    },
    _ => false.asRight[NonEmptyList[String]]
  )

  val monitorAndAlert = monitor _ andThen alert _

}
