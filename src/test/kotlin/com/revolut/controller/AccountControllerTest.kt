package com.revolut.controller

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.fuel.jackson.responseObject
import com.revolut.configuration.createDBPool
import com.revolut.configuration.createSchema
import com.revolut.configuration.createServer
import com.revolut.dao.Account
import io.javalin.Javalin
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Account API")
class AccountControllerTest  {

    private lateinit var app: Javalin

    @BeforeAll
    fun setUp() {
        createDBPool()
        app = createServer().start()
        val manager : FuelManager = FuelManager.instance.apply {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            })

            socketFactory = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, java.security.SecureRandom())
            }.socketFactory

            hostnameVerifier = HostnameVerifier { _, _ -> true }
        }
        FuelManager.instance.basePath = "https://localhost:${app.port()}/"
    }

    @AfterAll
    fun tearDown() {
        app.stop()
    }

    @BeforeEach
    fun fillTable() {
        createSchema()
        "/api/1/account".httpPut().response()
        "/api/1/account".httpPut().response()
        "/api/1/account".httpPut().response()
        "/api/1/account".httpPut().response()
        "/api/1/account".httpPut().response()
    }

    @AfterEach
    fun truncateTable() {
        transaction {
            TransactionManager.current().exec("DROP ALL OBJECTS")
        }
    }

    @Test
    fun `create new account`() {
        val (_, _, result) = "/api/1/account".httpPut().responseObject<Account>()

        assertEquals(6, result.get().id)
        assertEquals(0, result.get().amount)
    }

    @Test
    fun `should get all accounts`() {
        val (_, _, result) = "/api/1/account".httpGet().responseObject<List<Account>>()

        assertEquals(5, result.get().size)
    }

    @Test
    fun `should get account`() {
        val (_, _, result) = "/api/1/account/1".httpGet().responseObject<Account>()

        assertEquals(1, result.get().id)
        assertEquals(0, result.get().amount)
    }

    @Test
    fun `should get error for non-existing account`() {
        val (_, _, result) = "/api/1/account/-1".httpGet().responseObject<Account>()
        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `proper deposit should get incremented account`() {
        val (_, _, result) = "/api/1/account/deposit/1/10".httpPost().responseObject<Account>()

        assertEquals(1, result.get().id)
        assertEquals(10, result.get().amount)
    }

    @Test
    fun `deposit with negative amount should get error`() {
        val (_, _, result) = "/api/1/account/deposit/1/-10".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `deposit for unknown account should get error`() {
        val (_, _, result) = "/api/1/account/deposit/-1/10".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `proper withdraw should get decremented account`() {
        "/api/1/account/deposit/1/10".httpPost().response()
        val (_, _, result) = "/api/1/account/withdraw/1/3".httpPost().responseObject<Account>()

        assertEquals(1, result.get().id)
        assertEquals(7, result.get().amount)
    }

    @Test
    fun `withdraw with negative amount should get error`() {
        "/api/1/account/deposit/1/10".httpPost()
        val (_, _, result) = "/api/1/account/withdraw/1/-1".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `withdraw for very big amount should get error`() {
        "/api/1/account/deposit/1/10".httpPost()
        val (_, _, result) = "/api/1/account/withdraw/1/20".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `withdraw for unknown account should get error`() {
        val (_, _, result) = "/api/1/account/withdraw/-1/10".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `proper transfer should get decremented account and incremented amounts`() {
        "/api/1/account/deposit/1/10".httpPost().response()
        "/api/1/account/transfer/1/2/3".httpPost().response()
        val (_, _, from) = "/api/1/account/1".httpGet().responseObject<Account>()

        assertEquals(1, from.get().id)
        assertEquals(7, from.get().amount)

        val (_, _, to) = "/api/1/account/2".httpGet().responseObject<Account>()

        assertEquals(2, to.get().id)
        assertEquals(3, to.get().amount)
    }

    @Test
    fun `proper transfer from account with greater id should get decremented account and incremented amounts`() {
        "/api/1/account/deposit/2/10".httpPost().response()
        "/api/1/account/transfer/2/1/7".httpPost().response()
        val (_, _, from) = "/api/1/account/1".httpGet().responseObject<Account>()

        assertEquals(1, from.get().id)
        assertEquals(7, from.get().amount)

        val (_, _, to) = "/api/1/account/2".httpGet().responseObject<Account>()

        assertEquals(2, to.get().id)
        assertEquals(3, to.get().amount)
    }

    @Test
    fun `transfer from unknown account should get error`() {
        "/api/1/account/deposit/2/10".httpPost().response()
        val (_, _, result) = "/api/1/account/transfer/-2/1/1".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `transfer to unknown account should get error`() {
        "/api/1/account/deposit/2/10".httpPost().response()
        val (_, _, result) = "/api/1/account/transfer/2/-1/1".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }

    @Test
    fun `transfer big amount should get error`() {
        "/api/1/account/deposit/2/10".httpPost().response()
        val (_, _, result) = "/api/1/account/transfer/2/1/100".httpPost().responseObject<Account>()

        val (_, error) = result

        assertEquals(400, error!!.response.statusCode)
    }
}