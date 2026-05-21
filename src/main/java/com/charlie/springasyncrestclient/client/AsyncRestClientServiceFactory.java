package com.charlie.springasyncrestclient.client;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AsyncRestClientServiceFactory {

    private final RestClient.Builder restClientBuilder;
    private final Executor restClientExecutor;

    public AsyncRestClientServiceFactory(
            RestClient.Builder restClientBuilder,
            Executor restClientExecutor
    ) {
        this.restClientBuilder = restClientBuilder;
        this.restClientExecutor = restClientExecutor;
    }

    public AsyncRestClientService create(String baseUrl) {
        return new AsyncRestClientService(
                restClientBuilder,
                restClientExecutor,
                baseUrl
        );
    }
}
