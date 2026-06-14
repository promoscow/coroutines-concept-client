package ru.chernyshoff.client.dao.client.impl

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import ru.chernyshoff.client.dao.client.ServerClient
import ru.chernyshoff.client.dao.client.mapper.toRequest
import ru.chernyshoff.client.dao.client.mapper.toTrace
import ru.chernyshoff.client.dao.client.model.TraceResponse
import ru.chernyshoff.client.domain.Trace

@Component
class ServerClientImpl(
    private val client: WebClient,
    @Value($$"${app.server.host}") private val serverHost: String
) : ServerClient {

    override suspend fun trace(trace: Trace): Trace =
        client
            .post()
            .uri("$serverHost/server/io/trace")
            .bodyValue(trace.toRequest())
            .retrieve()
            .bodyToMono(TraceResponse::class.java)
            .awaitSingle()
            .toTrace()
}