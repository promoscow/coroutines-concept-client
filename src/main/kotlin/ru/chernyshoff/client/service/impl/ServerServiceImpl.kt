package ru.chernyshoff.client.service.impl

import org.springframework.stereotype.Service
import ru.chernyshoff.client.dao.client.ServerClient
import ru.chernyshoff.client.service.ServerService

@Service
class ServerServiceImpl(
    private val client: ServerClient
) : ServerService {

    override suspend fun get(taskId: String): String = client.get(taskId)
}