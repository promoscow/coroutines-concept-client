package ru.chernyshoff.client.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.chernyshoff.client.domain.Trace
import ru.chernyshoff.client.domain.type.ServiceType
import ru.chernyshoff.client.service.ClientTasksStartingService
import ru.chernyshoff.client.service.ServerService

@EnableScheduling
@Service
class ClientTasksStartingServiceImpl(
    private val service: ServerService,
    @Value($$"${app.service-prefix}") private val servicePrefix: String,
    @Value($$"${app.requests-per-second}") private val requestsPerSecond: Int
) : ClientTasksStartingService {

    private val logger = KotlinLogging.logger { this::class.java }

    @Scheduled(cron = "* * * * * *")
    override fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            (0 until requestsPerSecond).map { _ ->
                launch {
                    val trace = Trace(
                        traceId = "${servicePrefix}.${RandomStringUtils.secure().nextAlphanumeric(6)}",
                        service = ServiceType.CLIENT
                    )
                    service.trace(trace)
                }
            }
        }
        logger.info { "Started $requestsPerSecond tasks" }
    }
}