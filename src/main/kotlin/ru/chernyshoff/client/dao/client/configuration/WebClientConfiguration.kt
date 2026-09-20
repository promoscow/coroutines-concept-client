package ru.chernyshoff.client.dao.client.configuration

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfiguration {

    @Bean
    fun webClient(meterRegistry: MeterRegistry): WebClient {
        val observationRegistry = ObservationRegistry.create()
        observationRegistry.observationConfig().observationHandler(DefaultMeterObservationHandler(meterRegistry))
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient()))
            .observationRegistry(observationRegistry)
            .defaultHeader("Accept", "application/json")
            .build()
    }

    private fun httpClient(): HttpClient {
        // Настройка пула соединений для высоких нагрузок
        val connectionProvider =
            ConnectionProvider
                .builder("server-connection-pool")
                .maxConnections(100000)
                .pendingAcquireMaxCount(1000000)
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(120))
                .build()

        return HttpClient
            .create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
            .responseTimeout(Duration.ofSeconds(60))
            .doOnConnected { connection ->
                connection
                    .addHandlerLast(ReadTimeoutHandler(60, TimeUnit.SECONDS))
                    .addHandlerLast(WriteTimeoutHandler(30, TimeUnit.SECONDS))
            }
            .compress(true)
            .wiretap(false)
    }
}