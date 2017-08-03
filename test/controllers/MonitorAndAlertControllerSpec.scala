package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._

class MonitorAndLertControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "MonitorAndAllert POST" should {
    "process event" in {
      val controller = inject[MonitorAndAlertController]
      val result = controller.push().apply(FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "blah": "blahblah" }""")))

      status(result) mustBe CREATED
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) must include ("blah")
    }
  }
}
