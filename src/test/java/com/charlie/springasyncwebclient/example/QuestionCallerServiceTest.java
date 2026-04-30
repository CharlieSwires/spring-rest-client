package com.charlie.springasyncwebclient.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.charlie.springasyncwebclient.client.AsyncWebClientService;
import com.charlie.springasyncwebclient.dto.AnswerResponse;
import com.charlie.springasyncwebclient.dto.QuestionRequest;

import reactor.core.publisher.Mono;

class QuestionCallerServiceTest {

    private AsyncWebClientService asyncWebClientService;
    private QuestionCallerService questionCallerService;

    @BeforeEach
    void setUp() {
        asyncWebClientService = mock(AsyncWebClientService.class);
        questionCallerService = new QuestionCallerService(asyncWebClientService);
    }

    @Test
    void askQuestionUsingWebClientReturnsAnswerWhenCallSucceeds() {
        AnswerResponse mockResponse = new AnswerResponse();
        mockResponse.setAnswer("Spring Boot helps build Java applications.");
        mockResponse.setScore(0.95);

        when(asyncWebClientService.asyncPost(
                eq("/api/answer"),
                any(QuestionRequest.class),
                eq(AnswerResponse.class),
                eq(Duration.ofSeconds(10))
        )).thenReturn(CompletableFuture.completedFuture(mockResponse));

        AnswerResponse result = questionCallerService
                .askQuestionUsingWebClient()
                .join();

        assertEquals("Spring Boot helps build Java applications.", result.getAnswer());
        assertEquals(0.95, result.getScore());

        ArgumentCaptor<QuestionRequest> requestCaptor =
                ArgumentCaptor.forClass(QuestionRequest.class);

        verify(asyncWebClientService).asyncPost(
                eq("/api/answer"),
                requestCaptor.capture(),
                eq(AnswerResponse.class),
                eq(Duration.ofSeconds(10))
        );

        QuestionRequest capturedRequest = requestCaptor.getValue();

        assertEquals("What is Spring Boot?", capturedRequest.getQuestion());
        assertEquals(
                "Spring Boot is a Java framework for building web applications.",
                capturedRequest.getContext()
        );
    }

    @Test
    void askQuestionUsingWebClientReturnsFallbackWhenCallFails() {
        CompletableFuture<AnswerResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Remote API failed"));

        when(asyncWebClientService.asyncPost(
                eq("/api/answer"),
                any(QuestionRequest.class),
                eq(AnswerResponse.class),
                eq(Duration.ofSeconds(10))
        )).thenReturn(failedFuture);

        AnswerResponse result = questionCallerService
                .askQuestionUsingWebClient()
                .join();

        assertEquals(
                "No answer available because the remote service failed.",
                result.getAnswer()
        );
        assertEquals(0.0, result.getScore());
    }
    
    @Test
    void asyncPostUsesBaseUrlAndUriTogether() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> {
                    capturedUri.set(clientRequest.url());

                    ClientResponse response = ClientResponse
                            .create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body("""
                                    {
                                      "answer": "OK",
                                      "score": 1.0
                                    }
                                    """)
                            .build();

                    return Mono.just(response);
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

        assertEquals("OK", result.getAnswer());
        assertEquals(1.0, result.getScore());

        assertEquals(
                URI.create("https://example.com/api/answer"),
                capturedUri.get()
        );
    }

    @Test
    void asyncGetUsesBaseUrlUriAndQueryParametersTogether() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(clientRequest -> {
                    capturedUri.set(clientRequest.url());

                    ClientResponse response = ClientResponse
                            .create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body("""
                                    {
                                      "answer": "GET OK",
                                      "score": 0.75
                                    }
                                    """)
                            .build();

                    return Mono.just(response);
                });

        AsyncWebClientService service = new AsyncWebClientService(
                webClientBuilder,
                "https://example.com"
        );

        AnswerResponse result = service.asyncGet(
                        "/api/answer",
                        Map.of("id", 123),
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals("GET OK", result.getAnswer());
        assertEquals(0.75, result.getScore());

        assertEquals(
                URI.create("https://example.com/api/answer?id=123"),
                capturedUri.get()
        );
    }
}
