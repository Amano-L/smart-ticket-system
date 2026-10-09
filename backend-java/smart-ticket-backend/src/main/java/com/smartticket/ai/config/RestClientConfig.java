package com.smartticket.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${python.agent.url}")
    private String pythonAgentUrl;

    @Value("${python.agent.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${python.agent.read-timeout-ms}")
    private int readTimeoutMs;

    @Bean("pythonAgentRestClient")
    public RestClient pythonAgentRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(pythonAgentUrl)
                .build();
    }
}