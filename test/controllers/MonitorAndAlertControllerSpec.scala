package controllers

import akka.stream.Materializer
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._

class MonitorAndLertControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  implicit lazy val materializer: Materializer = app.materializer

  "MonitorAndAllert POST" should {
    "process event" in {
      val controller = inject[MonitorAndAlertController]
      val result = controller.push().apply(FakeRequest(POST, "/")
        //.withHeaders(CONTENT_TYPE -> JSON)
        .withJsonBody(Json.parse("""{ "blah": "blahblah" }"""))
      )

      status(result) mustBe CREATED
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) must include ("blah")
    }
  }
}
