package com.revolut.configuration

import com.revolut.dao.AccountTable
import org.jetbrains.exposed.sql.Database
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction


fun createDBPool() {
    val config = HikariConfig()
    config.driverClassName = appConfig[jdbcDriver]
    config.jdbcUrl = appConfig[jdbcUrl]
    Database.connect(HikariDataSource(config))
}

fun createSchema() {
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(AccountTable)
    }
}

