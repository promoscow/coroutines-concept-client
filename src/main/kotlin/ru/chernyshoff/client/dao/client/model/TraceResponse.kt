package ru.chernyshoff.client.dao.client.model

import ru.chernyshoff.client.dao.client.model.type.ServiceTypeDto
import java.time.OffsetDateTime

data class TraceResponse(
    val traceId: String,
    val timestamp: OffsetDateTime,
    val responseService: ServiceTypeDto
)
