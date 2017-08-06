package controllers

import cats.data.NonEmptyList
import cats.syntax.either._
import io.circe.Encoder
import java.time.{ Duration, Instant }
import play.api.libs.circe.Circe
import play.api.mvc.{ AbstractController, Action, ControllerComponents, Results }
import scala.collection.mutable.{ Map => MMap }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._

import Base._

class MonitorAndAlertController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Circe {

  def push() = Action(circe.json[Event]).async { implicit request =>
    val event = request.body

    import EventRepositoryInMemoryEmpty._
    import Alert._

    // start and end should be calculated in other place
    val end = event.creationDate.instant
    // todo: time period should be taken from config
    val start = end.minus(Duration.ofMillis(30 * 60 * 1000L))

    // todo: store and query on db should be done in other place
    // add effect of Asynch (Future, Task, Observalbe)
    val e = for {
      _ <- store(event)
      events <- queryBy(event.userName, Failed, (start, end))
      _ <- alert(events)
    } yield events

    // here alert will be printed
    e.leftMap(nel => println(nel.head))

    Future(Results.Created)
  }

}

trait EventRepository {
  import Base._

  type TimePeriod = (Instant, Instant)

  def store(event: Event): ErrorOr[Event]
  def queryBy(userName: String, status: Status, timePeriod: TimePeriod): ErrorOr[Seq[Event]]
}

trait EventRepositoryInMemory extends EventRepository {
  lazy val repo = MMap.empty[String, Seq[Event]]

  // store is optimized for use case of retriving events by user name
  // and their filtering if they got into time period
  // Improvments:
  // 1. Set can be used to achive idempotancy of events (event id should be used for duplicate recognition)
  // 2. Sorted collection (by creation date) can be used to optimize retrival/filtering
  def store(event: Event): ErrorOr[Event] = {
    repo += (event.userName -> (event +: repo.getOrElse(event.userName, Seq.empty[Event])))
    event.asRight[NonEmptyList[String]]
  }

  def queryBy(userName: String, status: Status, timePeriod: TimePeriod): ErrorOr[Seq[Event]] = {
     repo.getOrElse(userName, Seq.empty[Event]).filter { event =>

       // exclusive start instant, inclusive end instant
       def isInTimePeriod(creationDateInstant: Instant) = creationDateInstant.isAfter(timePeriod._1) &&
         (creationDateInstant.isBefore(timePeriod._2) || creationDateInstant.equals(timePeriod._2))

       event match {
         case Event(_, _, _, _, _, _, creationDate, status) if isInTimePeriod(creationDate.instant) => true
         case _ => false
       }
    }.asRight[NonEmptyList[String]]
  }
}

object EventRepositoryInMemoryEmpty extends EventRepositoryInMemory

// test example
object EventRepositoryInMemoryTest extends EventRepositoryInMemory {
  override lazy val repo = MMap.empty[String, Seq[Event]]
}

object Base {
  import io.circe.{ Decoder, HCursor }
  import io.circe.generic.auto._
  import io.circe.generic.extras._
  import io.circe.syntax._

  type ErrorOr[A] = Either[NonEmptyList[String], A]

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

}

object Alert {
  import Base._

  val MAX_EVENTS_BEFORE_ALERT = 10

  // alert returns Left, if it gets more then MAX_EVENTS_BEFORE_ALERT
  def alert(events: Seq[Event]): ErrorOr[Seq[Event]] =
    if (events.size >= MAX_EVENTS_BEFORE_ALERT)
      NonEmptyList.one(s"!!!Alert - user failed to login more then $MAX_EVENTS_BEFORE_ALERT times").asLeft[Seq[Event]]
    else
      events.asRight[NonEmptyList[String]]

}
