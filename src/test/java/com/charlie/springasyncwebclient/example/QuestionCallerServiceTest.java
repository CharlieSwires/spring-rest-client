package com.charlie.springasyncwebclient.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import com.charlie.springasyncwebclient.client.AsyncWebClientService;
import com.charlie.springasyncwebclient.dto.AnswerResponse;
import com.charlie.springasyncwebclient.dto.QuestionRequest;

class QuestionCallerServiceTest {

    private FakeAsyncWebClientService asyncWebClientService;
    private QuestionCallerService questionCallerService;

    @BeforeEach
    void setUp() {
        asyncWebClientService = new FakeAsyncWebClientService();
        questionCallerService = new QuestionCallerService(asyncWebClientService);
    }

    @Test
    void askQuestionUsingWebClientReturnsAnswerWhenCallSucceeds() {
        AnswerResponse mockResponse = new AnswerResponse();
        mockResponse.setAnswer("Spring Boot helps build Java applications.");
        mockResponse.setScore(0.95);

        asyncWebClientService.responseToReturn = CompletableFuture.completedFuture(mockResponse);

        AnswerResponse result = questionCallerService
                .askQuestionUsingWebClient()
                .join();

        assertEquals("Spring Boot helps build Java applications.", result.getAnswer());
        assertEquals(0.95, result.getScore());

        assertEquals("/api/answer", asyncWebClientService.capturedUri);
        assertEquals(AnswerResponse.class, asyncWebClientService.capturedResponseType);
        assertEquals(Duration.ofSeconds(10), asyncWebClientService.capturedTimeout);

        QuestionRequest capturedRequest = asyncWebClientService.capturedPayload;

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

        asyncWebClientService.responseToReturn = failedFuture;

        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

        try {
            System.setErr(new PrintStream(capturedErr));

            AnswerResponse result = questionCallerService
                    .askQuestionUsingWebClient()
                    .join();

            assertEquals(
                    "No answer available because the remote service failed.",
                    result.getAnswer()
            );
            assertEquals(0.0, result.getScore());
        } finally {
            System.setErr(originalErr);
        }
    }

    private static class FakeAsyncWebClientService extends AsyncWebClientService {

        private String capturedUri;
        private QuestionRequest capturedPayload;
        private Class<?> capturedResponseType;
        private Duration capturedTimeout;
        private CompletableFuture<AnswerResponse> responseToReturn;

        FakeAsyncWebClientService() {
            super(WebClient.builder(), "https://unused.example.com");
        }

        @Override
        public <R> CompletableFuture<R> asyncGet(
                String uri,
                Map<String, ?> queryParams,
                Class<R> responseType,
                Duration timeout
        ) {
            throw new UnsupportedOperationException("This test only expects asyncPost to be called.");
        }

        @Override
        public <T, R> CompletableFuture<R> asyncPost(
                String uri,
                T payload,
                Class<R> responseType,
                Duration timeout
        ) {
            this.capturedUri = uri;
            this.capturedPayload = (QuestionRequest) payload;
            this.capturedResponseType = responseType;
            this.capturedTimeout = timeout;

            @SuppressWarnings("unchecked")
            CompletableFuture<R> typedResponse = (CompletableFuture<R>) responseToReturn;
            return typedResponse;
        }

        @Override
        public <T, R> CompletableFuture<R> asyncPostGeneric(
                String uri,
                T payload,
                ParameterizedTypeReference<R> responseType,
                Duration timeout
        ) {
            throw new UnsupportedOperationException("This test only expects asyncPost to be called.");
        }
    }
}
