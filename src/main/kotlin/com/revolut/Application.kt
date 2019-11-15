package com.revolut

import com.revolut.configuration.createDBPool
import com.revolut.configuration.createSchema
import com.revolut.configuration.createServer

fun main() {
    createDBPool()
    createSchema()
    createServer().start()
}