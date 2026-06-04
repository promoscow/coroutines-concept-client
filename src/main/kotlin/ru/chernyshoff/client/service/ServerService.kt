package ru.chernyshoff.client.service

interface ServerService {

    suspend fun get(taskId: String): String
}