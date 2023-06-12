package repository

import model.Account
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future

class Repository(val accountId: Int, defAmount: Int){
    var accounts: Map[Int, Account] = Map.empty
    var lastAccountId: Int = ConfigFactory.load().getInt("account.id")
    lastAccountId *= 100

    def createAccount(): Future[Account] = {
        val accountId = generateAccountId()
        val account = Account(accountId, defAmount)
        accounts += (accountId -> account)
        Future.successful(account)
    }

    def getAccount(accountId: Int): Option[Account] = {
        accounts.get(accountId)
    }

    private def generateAccountId(): Int = {
        lastAccountId += 1
        lastAccountId
    }

    def update(accountId: Int, value: Int): Future[Account] = {
        accounts.get(accountId) match {
            case Some(account) =>
                val updatedAccount = account.update(value)
                accounts += (accountId -> updatedAccount)
                Future.successful(updatedAccount)
            case None => Future.failed(new Exception(s"Аккаунт с ID $accountId не найден"))
        }
    }
}
