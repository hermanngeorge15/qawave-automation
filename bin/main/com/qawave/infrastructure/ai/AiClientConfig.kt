package com.qawave.infrastructure.ai

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Configuration for AI client bean wiring.
 * Provides the delegate AI client for the resilient wrapper.
 */
@Configuration
class AiClientConfig {

    /**
     * Provides the OpenAI client as the delegate when provider is 'openai'.
     */
    @Bean("delegateAiClient")
    @ConditionalOnProperty(name = ["qawave.ai.provider"], havingValue = "openai", matchIfMissing = true)
    fun openAiDelegateClient(openAiClient: OpenAiClient): AiClient {
        return openAiClient
    }

    /**
     * Provides the stub client as the delegate when provider is 'stub'.
     */
    @Bean("delegateAiClient")
    @ConditionalOnProperty(name = ["qawave.ai.provider"], havingValue = "stub")
    fun stubDelegateClient(stubAiClient: StubAiClient): AiClient {
        return stubAiClient
    }
}
