package com.revolut.dao

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Column

data class Account(val id: Long, val amount:Long)

object AccountTable: LongIdTable("account") {
    val amount: Column<Long> = long("amount")
}

class AccountDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AccountDao>(AccountTable)

    var amount by AccountTable.amount

    fun toModel(): Account {
        return Account(id.value, amount)
    }
}