package com.charlie.springasyncwebclient.client;


import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AsyncWebClientServiceFactory {

    private final WebClient.Builder webClientBuilder;

    public AsyncWebClientServiceFactory(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public AsyncWebClientService create(String baseUrl) {
        return new AsyncWebClientService(webClientBuilder, baseUrl);
    }
}