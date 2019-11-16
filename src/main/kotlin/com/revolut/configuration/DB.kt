package com.revolut.configuration

import com.revolut.dao.AccountTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction


fun createDBPool() {
    val config = HikariConfig()
    config.driverClassName = appConfig[jdbcDriver]
    config.jdbcUrl = appConfig[jdbcUrl]
    Database.connect(HikariDataSource(config))
}

fun createSchema() {
    transaction {
        addLogger(Slf4jSqlInfoLogger)
        SchemaUtils.create(AccountTable)
    }
}

object Slf4jSqlInfoLogger : SqlLogger {
    override fun log (context: StatementContext, transaction: Transaction) {
        if (exposedLogger.isInfoEnabled) {
            exposedLogger.info(context.expandArgs(TransactionManager.current()))
        }
    }
}


