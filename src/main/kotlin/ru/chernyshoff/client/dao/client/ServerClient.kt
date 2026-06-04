package ru.chernyshoff.client.dao.client

interface ServerClient {

    suspend fun get(taskId: String): String
}