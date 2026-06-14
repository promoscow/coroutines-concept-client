package ru.chernyshoff.client.service

import ru.chernyshoff.client.domain.Trace

interface ServerService {

    suspend fun trace(trace: Trace): Trace
}