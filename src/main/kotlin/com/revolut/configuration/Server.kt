package com.revolut.configuration

import com.revolut.controller.AccountController
import com.revolut.controller.addEndpoint
import io.javalin.Javalin
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.swagger.v3.oas.models.info.Info
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

fun createServer(): Javalin {
    val app = Javalin.create {config ->
        config.defaultContentType = "application/json"
        config.enableCorsForAllOrigins()
        config.registerPlugin(OpenApiPlugin(getOpenApiOptions()));
        config.server {
            val server = Server()
            val sslConnector = ServerConnector(server, sslContextFactory())
            sslConnector.port = appConfig[sslPort]
            server.connectors = arrayOf<Connector>(sslConnector)
            server
        }
    }

    addEndpoint(app)
    return app
}

private fun sslContextFactory(): SslContextFactory {
    val sslContextFactory = SslContextFactory()
    sslContextFactory.keyStorePath = AccountController::class.java.getResource(appConfig[keystore]).toExternalForm()
    sslContextFactory.setKeyStorePassword(appConfig[keystorePassword])
    return sslContextFactory
}

private fun getOpenApiOptions(): OpenApiOptions {
    val applicationInfo = Info()
        .version("1.0")
        .description("Account Api");
    return OpenApiOptions(applicationInfo).path("/swagger-docs");
}
