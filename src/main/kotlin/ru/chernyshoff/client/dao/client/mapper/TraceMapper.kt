package ru.chernyshoff.client.dao.client.mapper

import ru.chernyshoff.client.dao.client.model.TraceRequest
import ru.chernyshoff.client.dao.client.model.TraceResponse
import ru.chernyshoff.client.dao.client.model.type.ServiceTypeDto
import ru.chernyshoff.client.domain.Trace
import ru.chernyshoff.client.domain.type.ServiceType
import java.time.OffsetDateTime

fun Trace.toRequest(): TraceRequest = TraceRequest(
    traceId = this.traceId,
    timestamp = OffsetDateTime.now(),
    requestService = ServiceTypeDto.valueOf(this.service.name)
)

fun TraceResponse.toTrace(): Trace = Trace(
    traceId = this.traceId,
    service = ServiceType.valueOf(this.responseService.name)
)