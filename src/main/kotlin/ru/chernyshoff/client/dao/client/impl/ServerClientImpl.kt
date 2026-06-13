package ru.chernyshoff.client.dao.client.impl

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import ru.chernyshoff.client.dao.client.ServerClient

@Component
class ServerClientImpl(
    private val client: WebClient,
    @Value($$"${app.server.host}") private val serverHost: String
) : ServerClient {

    override suspend fun trace(traceId: String): String =
        client
            .get()
            .uri("$serverHost/server/io/trace/{traceId}", traceId)
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
}