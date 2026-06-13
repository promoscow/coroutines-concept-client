package ru.chernyshoff.client.dao.client.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider

@Configuration
class WebClientConfiguration {

    @Bean
    fun webClient(): WebClient {
        val connectionProvider = ConnectionProvider.builder("serverConnectionPool")
            .pendingAcquireMaxCount(100000)
            .build()
        val connector = ReactorClientHttpConnector(HttpClient.create(connectionProvider))
        return WebClient
            .builder()
            .clientConnector(connector)
            .defaultHeader("Accept", "application/json")
            .build()
    }

}