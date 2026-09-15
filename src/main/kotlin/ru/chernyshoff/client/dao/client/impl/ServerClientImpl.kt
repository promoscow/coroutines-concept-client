package ru.chernyshoff.client.dao.client.impl

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.netty.handler.timeout.TimeoutException
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import ru.chernyshoff.client.dao.client.ServerClient
import ru.chernyshoff.client.dao.client.mapper.toRequest
import ru.chernyshoff.client.dao.client.mapper.toTrace
import ru.chernyshoff.client.dao.client.model.TraceResponse
import ru.chernyshoff.client.domain.Trace

@Component
class ServerClientImpl(
    private val client: WebClient,
    @Value($$"${app.server.host}") private val serverHost: String,
    private val meterRegistry: MeterRegistry
) : ServerClient {

    override suspend fun trace(trace: Trace): Trace =
        client
            .post()
            .uri("$serverHost/server/io/trace")
            .bodyValue(trace.toRequest())
            .retrieve()
            .bodyToMono(TraceResponse::class.java)
            .withHttpMetrics(
                meterRegistry = meterRegistry,
                serviceName = "trace-service",
                endpoint = "/server/io/trace",
                method = "POST"
            )
            .awaitSingle()
            .toTrace()
}

fun <T : Any> Mono<T>.withHttpMetrics(
    meterRegistry: MeterRegistry,
    serviceName: String,
    endpoint: String,
    method: String = "POST"
): Mono<T> {

    // Таймер для измерения времени выполнения
    val requestTimer = Timer.builder("webclient.request.duration")
        .description("WebClient request duration")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("method", method)
        .publishPercentiles(0.5, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(meterRegistry)

    // Счетчики статусов
    val status2xxCounter = Counter.builder("webclient.response.status")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("status", "2xx")
        .register(meterRegistry)

    val status4xxCounter = Counter.builder("webclient.response.status")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("status", "4xx")
        .register(meterRegistry)

    val status5xxCounter = Counter.builder("webclient.response.status")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("status", "5xx")
        .register(meterRegistry)

    val timeoutCounter = Counter.builder("webclient.response.status")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("status", "timeout")
        .register(meterRegistry)

    val errorCounter = Counter.builder("webclient.response.status")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("status", "error")
        .register(meterRegistry)

    // Общий счетчик запросов
    val totalRequests = Counter.builder("webclient.requests.total")
        .tag("service", serviceName)
        .tag("endpoint", endpoint)
        .tag("method", method)
        .register(meterRegistry)

    val sample = Timer.start(meterRegistry)

    return this
        .doOnSubscribe {
            totalRequests.increment()
        }
        .doOnSuccess { response ->
            // Записываем время выполнения
            sample.stop(requestTimer)

            // Подсчет статусов (если ответ содержит статус)
            if (response is org.springframework.web.reactive.function.client.ClientResponse) {
                recordStatus(response.statusCode().value(), status2xxCounter,
                    status4xxCounter, status5xxCounter)
            }
        }
        .doOnError { error ->
            // Записываем время даже при ошибках
            sample.stop(requestTimer)

            when (error) {
                is TimeoutException -> timeoutCounter.increment()
                is java.util.concurrent.TimeoutException -> timeoutCounter.increment()
                is org.springframework.web.reactive.function.client.WebClientResponseException -> {
                    val statusCode = error.statusCode.value()
                    recordStatus(statusCode, status2xxCounter,
                        status4xxCounter, status5xxCounter)
                }
                else -> errorCounter.increment()
            }
        }
}

// Вспомогательная функция для записи статусов
private fun recordStatus(
    statusCode: Int,
    status2xx: Counter,
    status4xx: Counter,
    status5xx: Counter
) {
    when (statusCode) {
        in 200..299 -> status2xx.increment()
        in 400..499 -> status4xx.increment()
        in 500..599 -> status5xx.increment()
    }
}