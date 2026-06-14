package ru.chernyshoff.client.dao.client

import ru.chernyshoff.client.domain.Trace

/**
 * Клиент работы с компонентом Server.
 */
interface ServerClient {

    suspend fun trace(trace: Trace): Trace
}