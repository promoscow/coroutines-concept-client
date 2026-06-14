package ru.chernyshoff.client.dao.client.model

import ru.chernyshoff.client.dao.client.model.type.ServiceTypeDto
import java.time.OffsetDateTime

data class TraceRequest(
    val traceId: String,
    val timestamp: OffsetDateTime,
    val requestService: ServiceTypeDto
)