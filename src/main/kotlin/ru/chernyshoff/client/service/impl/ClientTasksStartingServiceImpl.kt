package ru.chernyshoff.client.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.chernyshoff.client.service.ClientTasksStartingService
import ru.chernyshoff.client.service.ServerService

@EnableScheduling
@Service
class ClientTasksStartingServiceImpl(
    private val service: ServerService
) : ClientTasksStartingService {

    private val logger = KotlinLogging.logger { this::class.java }

    @Scheduled(fixedDelay = 1000)
    override fun start() {
        val taskId = RandomStringUtils.secure().nextAlphanumeric(6)
        logger.info { "Starting task: $taskId" }
        runBlocking { service.get(taskId) }.also { logger.info { "Result: $it" } }
    }
}