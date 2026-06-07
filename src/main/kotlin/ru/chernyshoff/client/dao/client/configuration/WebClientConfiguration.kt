package ru.chernyshoff.client.dao.client.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration {

    @Bean
    fun webClient(): WebClient =
        WebClient
            .builder()
            .defaultHeader("Accept", "application/json")
            .build()
}