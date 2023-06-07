package route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

class Route(implicit ec: ExecutionContext) extends FailFastCirceSupport {

    def routes =
        (path("hello") & get) {
            complete("ok")
        }
}
