package com.charlie.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RestClientService {

    private final WebClient webClient;

    public RestClientService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8080")
                .build();
    }

    public <R> CompletableFuture<R> asyncGet(
            String uri,
            Map<String, ?> queryParams,
            Class<R> responseType
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
                .toFuture();
    }

    public <T, R> CompletableFuture<R> asyncPost(
            String uri,
            T payload,
            Class<R> responseType
    ) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(responseType)
                .toFuture();
    }

    public <R> CompletableFuture<R> asyncGetGeneric(
            String uri,
            Map<String, ?> queryParams,
            ParameterizedTypeReference<R> responseType
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
                .toFuture();
    }

    public <T, R> CompletableFuture<R> asyncPostGeneric(
            String uri,
            T payload,
            ParameterizedTypeReference<R> responseType
    ) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(responseType)
                .toFuture();
    }
}