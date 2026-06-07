package ru.chernyshoff.client.dao.client

/**
 * Клиент работы с компонентом Server.
 */
interface ServerClient {

    suspend fun trace(traceId: String): String
}