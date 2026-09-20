package ru.chernyshoff.client.dao.client.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.netty.channel.AbortedException
import reactor.netty.http.client.PrematureCloseException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * Итог исходящего HTTP-вызова. Значение [tag] попадает в тег `status` метрики
 * `webclient.response.status`.
 */
enum class HttpCallStatus(val tag: String) {

    SUCCESS_2XX("2xx"),
    CLIENT_ERROR_4XX("4xx"),
    SERVER_ERROR_5XX("5xx"),
    CONNECT_TIMEOUT("connect_timeout"),
    CONNECT_ERROR("connect_error"),
    DNS_ERROR("dns_error"),
    READ_TIMEOUT("read_timeout"),
    WRITE_TIMEOUT("write_timeout"),
    POOL_TIMEOUT("pool_timeout"),
    POOL_EXHAUSTED("pool_exhausted"),
    PREMATURE_CLOSE("premature_close"),
    ABORTED("aborted"),
    IO_ERROR("io_error"),
    TIMEOUT("timeout"),
    CANCELLED("cancelled"),
    ERROR("error");

    companion object {

        /**
         * Определяет итог вызова по исключению.
         *
         * WebClient оборачивает сбои транспорта в [org.springframework.web.reactive.function.client.WebClientRequestException],
         * поэтому проверять нужно всю цепочку cause, а не только верхнее исключение.
         */
        fun of(error: Throwable): HttpCallStatus {
            if (error is WebClientResponseException) {
                return ofStatusCode(error.statusCode.value())
            }

            var current: Throwable? = error
            var depth = 0
            while (current != null && depth < MAX_CAUSE_DEPTH) {
                val cause = current
                when {
                    cause is ReadTimeoutException -> return READ_TIMEOUT
                    cause is WriteTimeoutException -> return WRITE_TIMEOUT
                    // Наследник ConnectException, поэтому проверяется раньше него
                    cause is ConnectTimeoutException -> return CONNECT_TIMEOUT
                    cause is UnknownHostException -> return DNS_ERROR
                    cause is ConnectException -> return CONNECT_ERROR
                    // Наследник IOException, поэтому проверяется раньше него
                    cause is PrematureCloseException -> return PREMATURE_CLOSE
                    cause is AbortedException -> return ABORTED
                    // Лежит в reactor.netty.internal.shaded, поэтому сверяется по имени класса
                    cause.javaClass.simpleName == POOL_EXHAUSTED_EXCEPTION -> return POOL_EXHAUSTED
                    cause is TimeoutException ->
                        return if (cause.javaClass.simpleName == POOL_TIMEOUT_EXCEPTION) POOL_TIMEOUT else TIMEOUT
                    cause is IOException -> return IO_ERROR
                }
                current = cause.cause?.takeIf { it !== cause }
                depth++
            }
            return ERROR
        }

        private fun ofStatusCode(statusCode: Int): HttpCallStatus = when (statusCode) {
            in 200..299 -> SUCCESS_2XX
            in 400..499 -> CLIENT_ERROR_4XX
            in 500..599 -> SERVER_ERROR_5XX
            else -> ERROR
        }

        private const val POOL_TIMEOUT_EXCEPTION = "PoolAcquireTimeoutException"
        private const val POOL_EXHAUSTED_EXCEPTION = "PoolAcquirePendingLimitException"

        // Цепочка cause может быть зациклена, поэтому обход ограничен по глубине
        private const val MAX_CAUSE_DEPTH = 16
    }
}

/**
 * Метрики одного исходящего направления (сервис + эндпоинт + метод).
 *
 * Все счетчики регистрируются один раз при создании, включая счетчики тех итогов, которые
 * еще ни разу не встречались: иначе временные ряды появляются в Prometheus только после первой
 * ошибки, и `rate()` по ним даёт пропуски.
 */
class HttpCallMetrics(
    private val meterRegistry: MeterRegistry,
    serviceName: String,
    endpoint: String,
    method: String
) {

    private val requestTimer: Timer = Timer.builder("webclient.request.duration")
        .description("WebClient request duration")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("method", method)
        .publishPercentiles(0.5, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(meterRegistry)

    private val totalRequests: Counter = Counter.builder("webclient.requests.total")
        .description("Total WebClient requests")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("method", method)
        .register(meterRegistry)

    private val statusCounters: Map<HttpCallStatus, Counter> =
        HttpCallStatus.entries.associateWith { status ->
            Counter.builder("webclient.response.status")
                .description("WebClient responses by outcome")
                .tag("service", serviceName)
                .tag("endpoint", endpoint)
                .tag("status", status.tag)
                .register(meterRegistry)
        }

    fun startSample(): Timer.Sample = Timer.start(meterRegistry)

    fun stopSample(sample: Timer.Sample) {
        sample.stop(requestTimer)
    }

    fun recordStarted() {
        totalRequests.increment()
    }

    fun record(status: HttpCallStatus) {
        statusCounters.getValue(status).increment()
    }
}

/**
 * Снимает метрики исходящего вызова.
 *
 * Отсчёт времени начинается внутри [Mono.defer], чтобы каждая подписка получала свой замер,
 * а [Mono.doFinally] гарантирует остановку таймера на любом терминальном сигнале, включая отмену.
 */
fun <T : Any> Mono<T>.withHttpMetrics(metrics: HttpCallMetrics): Mono<T> {
    val source = this
    return Mono.defer {
        val sample = metrics.startSample()
        source
            .doOnSubscribe { metrics.recordStarted() }
            .doOnSuccess { metrics.record(HttpCallStatus.SUCCESS_2XX) }
            .doOnError { error -> metrics.record(HttpCallStatus.of(error)) }
            .doOnCancel { metrics.record(HttpCallStatus.CANCELLED) }
            .doFinally { metrics.stopSample(sample) }
    }
}
