package com.qawave.infrastructure.http

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Configuration for WebClient used in test execution.
 */
@Configuration
class WebClientConfig {

    @Value("\${qawave.http.connect-timeout-ms:5000}")
    private var connectTimeoutMs: Int = 5000

    @Value("\${qawave.http.response-timeout-ms:30000}")
    private var responseTimeoutMs: Long = 30000

    @Value("\${qawave.http.max-in-memory-size-mb:10}")
    private var maxInMemorySizeMb: Int = 10

    @Bean
    fun webClient(): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(responseTimeoutMs))

        val exchangeStrategies = ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(maxInMemorySizeMb * 1024 * 1024)
            }
            .build()

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(exchangeStrategies)
            .build()
    }
}
