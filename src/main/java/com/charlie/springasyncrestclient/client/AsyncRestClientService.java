package com.charlie.springasyncrestclient.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AsyncRestClientService {

    private final RestClient restClient;
    private final Executor restClientExecutor;

    public AsyncRestClientService(
            RestClient.Builder restClientBuilder,
            Executor restClientExecutor,
            String url
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = restClientBuilder
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build();

        this.restClientExecutor = restClientExecutor;
    }

    public <R> CompletableFuture<R> asyncGet(
            String uri,
            Map<String, ?> queryParams,
            Class<R> responseType,
            Duration timeout
    ) {
        return CompletableFuture
                .supplyAsync(() ->
                        restClient.get()
                                .uri(uriBuilder -> {
                                    uriBuilder.path(uri);

                                    if (queryParams != null) {
                                        queryParams.forEach(uriBuilder::queryParam);
                                    }

                                    return uriBuilder.build();
                                })
                                .retrieve()
                                .body(responseType),
                        restClientExecutor
                )
                .orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public <T, R> CompletableFuture<R> asyncPost(
            String uri,
            T payload,
            Class<R> responseType,
            Duration timeout
    ) {
        return CompletableFuture
                .supplyAsync(() ->
                        restClient.post()
                                .uri(uri)
                                .body(payload)
                                .retrieve()
                                .body(responseType),
                        restClientExecutor
                )
                .orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public <T, R> CompletableFuture<R> asyncPostGeneric(
            String uri,
            T payload,
            ParameterizedTypeReference<R> responseType,
            Duration timeout
    ) {
        return CompletableFuture
                .supplyAsync(() ->
                        restClient.post()
                                .uri(uri)
                                .body(payload)
                                .retrieve()
                                .body(responseType),
                        restClientExecutor
                )
                .orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}