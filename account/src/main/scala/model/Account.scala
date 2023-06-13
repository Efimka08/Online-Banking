package model

import java.util.UUID

case class Account(id: Int, amount: Int) {
    def update(value: Int) = this.copy(amount = amount + value)
}

trait Command
case class AccountUpdate(operationId: Int, accountId: Int, value: Int, category: Option[String])

trait Event
case class AccountUpdated(operationId: Int, accountId: Int, value: Int, category: Option[String])
