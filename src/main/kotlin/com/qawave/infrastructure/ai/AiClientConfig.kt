package com.qawave.infrastructure.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.qawave.application.port.AiClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Configuration for AI clients.
 */
@Configuration
class AiClientConfig {

    @Bean
    fun aiWebClient(
        @Value("\${qawave.ai.base-url:https://api.openai.com}") baseUrl: String,
        @Value("\${qawave.ai.timeout-seconds:60}") timeoutSeconds: Long
    ): WebClient {
        val httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(timeoutSeconds))

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) // 16MB
            }
            .build()
    }

    @Bean
    @Primary
    @Profile("!test")
    fun openAiClient(
        webClient: WebClient,
        objectMapper: ObjectMapper,
        @Value("\${qawave.ai.api-key:}") apiKey: String,
        @Value("\${qawave.ai.model:gpt-4o-mini}") model: String,
        @Value("\${qawave.ai.base-url:https://api.openai.com}") baseUrl: String
    ): AiClient {
        return OpenAiClient(webClient, objectMapper, apiKey, model, baseUrl)
    }

    @Bean
    @Primary
    @Profile("test")
    fun stubAiClient(): AiClient {
        return StubAiClient()
    }
}
