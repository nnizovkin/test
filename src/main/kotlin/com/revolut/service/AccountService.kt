package com.revolut.service

import com.revolut.configuration.Slf4jSqlInfoLogger
import com.revolut.dao.Account
import com.revolut.dao.AccountDao
import com.revolut.dao.AccountTable
import com.revolut.dao.execAndMap
import io.javalin.http.BadRequestResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object AccountService {
    fun create(): Account {
        return transaction {
            addLogger(Slf4jSqlInfoLogger)
            return@transaction AccountDao.new {
                amount = 0
            }.toModel()
        }
    }

    fun get(id: Long): Account {
        return transaction {
            addLogger(Slf4jSqlInfoLogger)
            val account = AccountDao.findById(id) ?: throw BadRequestResponse("there is no account with such id $id")
            return@transaction account.toModel()
        }
    }

    fun getAll(): List<Account> {
        return transaction {
            addLogger(Slf4jSqlInfoLogger)
            return@transaction AccountDao.all().limit(100).map { dao -> dao.toModel() }
        }
    }

    fun deposit(id: Long, amount: Long): Account {
        return transaction {
            addLogger(Slf4jSqlInfoLogger)
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
                TransactionManager.current().exec("select amount from account where id = $from for update")
                TransactionManager.current().exec("select amount from account where id = $to for update")
                withdraw(from, amount)
                deposit(to, amount)
            } else {
                TransactionManager.current().exec("select amount from account where id = $to for update")
                TransactionManager.current().exec("select amount from account where id = $from for update")
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