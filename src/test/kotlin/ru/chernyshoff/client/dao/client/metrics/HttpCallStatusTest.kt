package ru.chernyshoff.client.dao.client.metrics

import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.netty.channel.AbortedException
import reactor.netty.http.client.PrematureCloseException
import reactor.netty.internal.shaded.reactor.pool.PoolAcquireTimeoutException
import java.io.IOException
import java.net.ConnectException
import java.net.URI
import java.net.UnknownHostException
import java.time.Duration
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals

class HttpCallStatusTest {

    @Test
    fun `read timeout wrapped by WebClient is not reported as generic error`() {
        assertEquals(HttpCallStatus.READ_TIMEOUT, HttpCallStatus.of(wrapped(ReadTimeoutException.INSTANCE)))
    }

    @Test
    fun `write timeout is unwrapped from the WebClient wrapper`() {
        assertEquals(HttpCallStatus.WRITE_TIMEOUT, HttpCallStatus.of(wrapped(WriteTimeoutException.INSTANCE)))
    }

    @Test
    fun `connect timeout wins over its ConnectException supertype`() {
        assertEquals(
            HttpCallStatus.CONNECT_TIMEOUT,
            HttpCallStatus.of(wrapped(ConnectTimeoutException("connection timed out")))
        )
    }

    @Test
    fun `plain connect failure is reported separately from connect timeout`() {
        assertEquals(HttpCallStatus.CONNECT_ERROR, HttpCallStatus.of(wrapped(ConnectException("Connection refused"))))
    }

    @Test
    fun `unknown host wins over its IOException supertype`() {
        assertEquals(HttpCallStatus.DNS_ERROR, HttpCallStatus.of(wrapped(UnknownHostException("server"))))
    }

    @Test
    fun `premature close wins over its IOException supertype`() {
        assertEquals(
            HttpCallStatus.PREMATURE_CLOSE,
            HttpCallStatus.of(wrapped(PrematureCloseException.TEST_EXCEPTION))
        )
    }

    @Test
    fun `aborted connection is classified`() {
        assertEquals(HttpCallStatus.ABORTED, HttpCallStatus.of(wrapped(AbortedException("connection closed"))))
    }

    @Test
    fun `pool acquire timeout is distinguished from a generic timeout`() {
        assertEquals(
            HttpCallStatus.POOL_TIMEOUT,
            HttpCallStatus.of(wrapped(PoolAcquireTimeoutException(Duration.ofSeconds(60))))
        )
        assertEquals(HttpCallStatus.TIMEOUT, HttpCallStatus.of(wrapped(TimeoutException("timed out"))))
    }

    @Test
    fun `remaining IO failures fall back to io_error`() {
        assertEquals(HttpCallStatus.IO_ERROR, HttpCallStatus.of(wrapped(IOException("broken pipe"))))
    }

    @Test
    fun `response exceptions are classified by status code`() {
        assertEquals(HttpCallStatus.CLIENT_ERROR_4XX, HttpCallStatus.of(responseException(404)))
        assertEquals(HttpCallStatus.SERVER_ERROR_5XX, HttpCallStatus.of(responseException(503)))
    }

    @Test
    fun `unrecognised failures fall back to error`() {
        assertEquals(HttpCallStatus.ERROR, HttpCallStatus.of(IllegalStateException("boom")))
    }

    @Test
    fun `cause is found several levels deep`() {
        val nested = RuntimeException("outer", wrapped(ReadTimeoutException.INSTANCE))
        assertEquals(HttpCallStatus.READ_TIMEOUT, HttpCallStatus.of(nested))
    }

    @Test
    fun `cyclic cause chain terminates`() {
        val first = RuntimeException("first")
        val second = RuntimeException("second", first)
        first.initCause(second)

        assertEquals(HttpCallStatus.ERROR, HttpCallStatus.of(first))
    }

    private fun wrapped(cause: Throwable): WebClientRequestException =
        WebClientRequestException(cause, HttpMethod.POST, URI.create("http://server:8011/server/io/trace"), HttpHeaders())

    private fun responseException(statusCode: Int): WebClientResponseException =
        WebClientResponseException(statusCode, "status $statusCode", null, null, null)
}
