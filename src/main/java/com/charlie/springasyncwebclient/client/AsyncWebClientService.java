package com.charlie.springasyncwebclient.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AsyncWebClientService {

    private final WebClient webClient;

    public AsyncWebClientService(WebClient.Builder webClientBuilder, String url) {
        this.webClient = webClientBuilder
                .baseUrl(url)
                .build();
    }

    public <R> CompletableFuture<R> asyncGet(
            String uri,
            Map<String, ?> queryParams,
            Class<R> responseType,
            Duration timeout
    ) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(uri);

                    if (queryParams != null) {
                        queryParams.forEach(uriBuilder::queryParam);
                    }

                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(timeout)
                .toFuture();
    }

    public <R> CompletableFuture<R> asyncGetGeneric(
            String uri,
            Map<String, ?> queryParams,
            ParameterizedTypeReference<R> responseType,
            Duration timeout
    ) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(uri);

                    if (queryParams != null) {
                        queryParams.forEach(uriBuilder::queryParam);
                    }

                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(timeout)
                .toFuture();
    }

    public <T, R> CompletableFuture<R> asyncPost(
            String uri,
            T payload,
            Class<R> responseType,
            Duration timeout
    ) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(timeout)
                .toFuture();
    }

    public <T, R> CompletableFuture<R> asyncPostGeneric(
            String uri,
            T payload,
            ParameterizedTypeReference<R> responseType,
            Duration timeout
    ) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(timeout)
                .toFuture();
    }
}