package route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import model.Account
import repository.Repository

import scala.concurrent.ExecutionContext

class Route(repository: Repository)(implicit ec: ExecutionContext) extends FailFastCirceSupport {

  def routes =
      (path("hello") & get) {
          complete("ok")
      } ~
        (path("create") & post) {
            val createdAccount = repository.createAccount()
            complete(StatusCodes.Created, createdAccount)
        } ~
        (path("info" / IntNumber) { accountId =>
            complete {
                repository.getAccount(accountId) match {
                case Some(account) => account
                case None => StatusCodes.NotFound -> "Аккаунт не найден"
                }
            }
        })

}


