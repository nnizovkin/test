package com.revolut.configuration

import com.natpryce.konfig.*

val appConfig = ConfigurationProperties.systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromResource("default.properties")
val sslPort = Key("ssl.port", intType)
val keystore = Key("keystore", stringType)
val keystorePassword = Key("keystore.password", stringType)
val jdbcDriver = Key("jdbc.driver", stringType)
val jdbcUrl = Key("jdbc.url", stringType)
