package ru.chernyshoff.client.service

interface ServerService {

    suspend fun trace(traceId: String): String
}