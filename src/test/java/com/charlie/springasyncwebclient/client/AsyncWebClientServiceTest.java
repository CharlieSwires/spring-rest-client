package com.charlie.springasyncwebclient.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.charlie.springasyncwebclient.dto.AnswerResponse;
import com.charlie.springasyncwebclient.dto.QuestionRequest;

import reactor.core.publisher.Mono;

class AsyncWebClientServiceTest {

    @Test
    void asyncGetUsesBaseUrlUriAndQueryParametersTogether() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        AtomicReference<HttpMethod> capturedMethod = new AtomicReference<>();

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> {
                    capturedUri.set(clientRequest.url());
                    capturedMethod.set(clientRequest.method());

                    return Mono.just(jsonResponse("""
                            {
                              "answer": "GET OK",
                              "score": 0.75
                            }
                            """));
                });

        AsyncWebClientService service = new AsyncWebClientService(
                webClientBuilder,
                "https://example.com"
        );

        AnswerResponse result = service.asyncGet(
                        "/api/answer",
                        Map.of("id", 123, "mode", "short"),
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals(HttpMethod.GET, capturedMethod.get());
        assertEquals("GET OK", result.getAnswer());
        assertEquals(0.75, result.getScore());
        assertEquals(
                URI.create("https://example.com/api/answer?id=123&mode=short"),
                capturedUri.get()
        );
    }

    @Test
    void asyncGetAllowsNullQueryParameters() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> {
                    capturedUri.set(clientRequest.url());

                    return Mono.just(jsonResponse("""
                            {
                              "answer": "GET OK",
                              "score": 0.80
                            }
                            """));
                });

        AsyncWebClientService service = new AsyncWebClientService(
                webClientBuilder,
                "https://example.com"
        );

        AnswerResponse result = service.asyncGet(
                        "/api/answer",
                        null,
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals("GET OK", result.getAnswer());
        assertEquals(0.80, result.getScore());
        assertEquals(
                URI.create("https://example.com/api/answer"),
                capturedUri.get()
        );
    }

    @Test
    void asyncPostUsesBaseUrlUriJsonHeadersAndPayload() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        AtomicReference<HttpMethod> capturedMethod = new AtomicReference<>();
        AtomicReference<MediaType> capturedContentType = new AtomicReference<>();
        AtomicReference<List<MediaType>> capturedAccept = new AtomicReference<>();

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> {
                    capturedUri.set(clientRequest.url());
                    capturedMethod.set(clientRequest.method());
                    capturedContentType.set(clientRequest.headers().getContentType());
                    capturedAccept.set(clientRequest.headers().getAccept());

                    return Mono.just(jsonResponse("""
                            {
                              "answer": "POST OK",
                              "score": 1.0
                            }
                            """));
                });

        AsyncWebClientService service = new AsyncWebClientService(
                webClientBuilder,
                "https://example.com"
        );

        QuestionRequest request = new QuestionRequest(
                "What is Spring Boot?",
                "Spring Boot is a Java framework."
        );

        AnswerResponse result = service.asyncPost(
                        "/api/answer",
                        request,
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals(HttpMethod.POST, capturedMethod.get());
        assertEquals(MediaType.APPLICATION_JSON, capturedContentType.get());
        assertEquals(List.of(MediaType.APPLICATION_JSON), capturedAccept.get());
        assertEquals("POST OK", result.getAnswer());
        assertEquals(1.0, result.getScore());
        assertEquals(
                URI.create("https://example.com/api/answer"),
                capturedUri.get()
        );
    }

    @Test
    void asyncPostGenericDeserialisesListResponse() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> Mono.just(jsonResponse("""
                        [
                          { "answer": "First", "score": 0.9 },
                          { "answer": "Second", "score": 0.8 }
                        ]
                        """)));

        AsyncWebClientService service = new AsyncWebClientService(
                webClientBuilder,
                "https://example.com"
        );

        QuestionRequest request = new QuestionRequest("Question?", "Context");

        List<AnswerResponse> result = service.asyncPostGeneric(
                        "/api/answers",
                        request,
                        new ParameterizedTypeReference<List<AnswerResponse>>() {
                        },
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals(2, result.size());
        assertEquals("First", result.get(0).getAnswer());
        assertEquals(0.9, result.get(0).getScore());
        assertEquals("Second", result.get(1).getAnswer());
        assertEquals(0.8, result.get(1).getScore());
    }

    @Test
    void asyncGetTimesOutWhenRemoteCallIsTooSlow() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> Mono
                        .delay(Duration.ofMillis(250))
                        .thenReturn(jsonResponse("""
                                {
                                  "answer": "Too slow",
                                  "score": 0.1
                                }
                                """)));

        AsyncWebClientService service = new AsyncWebClientService(
                webClientBuilder,
                "https://example.com"
        );

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> service.asyncGet(
                                "/api/answer",
                                null,
                                AnswerResponse.class,
                                Duration.ofMillis(50)
                        )
                        .join()
        );

        assertTrue(exception.getCause() instanceof TimeoutException);
    }

    @Test
    void factoryCreatesAUsableServiceForTheGivenBaseUrl() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> {
                    capturedUri.set(clientRequest.url());

                    return Mono.just(jsonResponse("""
                            {
                              "answer": "Factory OK",
                              "score": 1.0
                            }
                            """));
                });

        AsyncWebClientServiceFactory factory = new AsyncWebClientServiceFactory(webClientBuilder);
        AsyncWebClientService service = factory.create("https://factory.example.com");

        AnswerResponse result = service.asyncGet(
                        "/api/answer",
                        null,
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals("Factory OK", result.getAnswer());
        assertEquals(
                URI.create("https://factory.example.com/api/answer"),
                capturedUri.get()
        );
    }

    private static ClientResponse jsonResponse(String body) {
        return ClientResponse
                .create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }
}
