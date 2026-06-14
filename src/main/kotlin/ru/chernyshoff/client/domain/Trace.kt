package ru.chernyshoff.client.domain

import ru.chernyshoff.client.domain.type.ServiceType

data class Trace(
    val traceId: String,
    val service: ServiceType
)
