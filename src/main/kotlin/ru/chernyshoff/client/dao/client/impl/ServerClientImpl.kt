package ru.chernyshoff.client.dao.client.impl

import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import ru.chernyshoff.client.dao.client.ServerClient
import ru.chernyshoff.client.dao.client.mapper.toRequest
import ru.chernyshoff.client.dao.client.mapper.toTrace
import ru.chernyshoff.client.dao.client.metrics.HttpCallMetrics
import ru.chernyshoff.client.dao.client.metrics.withHttpMetrics
import ru.chernyshoff.client.dao.client.model.TraceResponse
import ru.chernyshoff.client.domain.Trace

@Component
class ServerClientImpl(
    private val client: WebClient,
    @Value($$"${app.server.host}") private val serverHost: String,
    meterRegistry: MeterRegistry
) : ServerClient {

    private val traceMetrics = HttpCallMetrics(
        meterRegistry = meterRegistry,
        serviceName = "trace-service",
        endpoint = TRACE_ENDPOINT,
        method = "POST"
    )

    override suspend fun trace(trace: Trace): Trace =
        client
            .post()
            .uri("$serverHost$TRACE_ENDPOINT")
            .bodyValue(trace.toRequest())
            .retrieve()
            .bodyToMono(TraceResponse::class.java)
            .withHttpMetrics(traceMetrics)
            .awaitSingle()
            .toTrace()

    private companion object {
        const val TRACE_ENDPOINT = "/server/io/trace"
    }
}
