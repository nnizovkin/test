package com.revolut.service

import com.revolut.dao.Account
import com.revolut.dao.AccountDao
import com.revolut.dao.AccountTable
import com.revolut.dao.execAndMap
import io.javalin.http.BadRequestResponse
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object AccountService {
    fun create(): Account {
        return transaction {
            return@transaction AccountDao.new {
                amount = 0
            }.toModel()
        }
    }

    fun get(id: Long): Account {
        return transaction {
            val account = AccountDao.findById(id) ?: throw BadRequestResponse("there is no account with such id $id")
            return@transaction account.toModel()
        }
    }

    fun getAll(): List<Account> {
        return transaction {
            return@transaction AccountDao.all().limit(100).map { dao -> Account(dao.id.value, dao.amount) }
        }
    }

    fun deposit(id: Long, amount: Long): Account {
        return transaction {
            val amounts = "select amount from account where id = $id for update".execAndMap { rs ->
                rs.getLong("amount")
            }

            if (amounts.isEmpty()) {
                throw BadRequestResponse("there is no account with such id $id")
            }

            val newAmount = amounts[0] + amount
            if (newAmount < 0) {
                throw BadRequestResponse("there is no enough money in account with id $id")
            }

            AccountTable.update({ AccountTable.id eq id }) {
                it[AccountTable.amount] = newAmount
            }

            return@transaction Account(id, newAmount)
        }
    }

    fun withdraw(id: Long, amount: Long): Account {
        return deposit(id, -amount)
    }

    fun transfer(from: Long, to: Long, amount: Long) {
        transaction {
            if (from < to) {
                withdraw(from, amount)
                deposit(to, amount)
            } else {
                deposit(to, amount)
                withdraw(from, amount)
            }
        }
    }

    fun delete(id: Long) {
        transaction {
            AccountTable.deleteWhere { AccountTable.id eq id }
        }
    }
}