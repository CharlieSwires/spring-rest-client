package com.charlie.springasyncrestclient.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import com.charlie.springasyncwebclient.dto.AnswerResponse;
import com.charlie.springasyncwebclient.dto.QuestionRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class AsyncRestClientServiceTest {

    private HttpServer server;
    private ExecutorService serverExecutor;
    private ExecutorService restClientExecutor;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        serverExecutor = Executors.newSingleThreadExecutor();
        restClientExecutor = Executors.newSingleThreadExecutor();
        server.setExecutor(serverExecutor);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
        if (restClientExecutor != null) {
            restClientExecutor.shutdownNow();
        }
    }

    @Test
    void asyncGetUsesBaseUrlUriAndQueryParametersTogether() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<URI> capturedUri = new AtomicReference<>();

        server.createContext("/api/answer", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedUri.set(exchange.getRequestURI());

            writeJson(exchange, 200, """
                    {
                      "answer": "GET OK",
                      "score": 0.75
                    }
                    """);
        });

        AsyncRestClientService service = newService(baseUrl);

        AnswerResponse result = service.asyncGet(
                        "/api/answer",
                        Map.of("id", 123, "mode", "short"),
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals("GET", capturedMethod.get());
        assertEquals("GET OK", result.getAnswer());
        assertEquals(0.75, result.getScore());
        assertEquals("/api/answer", capturedUri.get().getPath());
        assertTrue(capturedUri.get().getQuery().contains("id=123"));
        assertTrue(capturedUri.get().getQuery().contains("mode=short"));
    }

    @Test
    void asyncGetAllowsNullQueryParameters() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();

        server.createContext("/api/answer", exchange -> {
            capturedUri.set(exchange.getRequestURI());

            writeJson(exchange, 200, """
                    {
                      "answer": "GET OK",
                      "score": 0.8
                    }
                    """);
        });

        AsyncRestClientService service = newService(baseUrl);

        AnswerResponse result = service.asyncGet(
                        "/api/answer",
                        null,
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals("GET OK", result.getAnswer());
        assertEquals(0.8, result.getScore());
        assertEquals("/api/answer", capturedUri.get().toString());
    }

    @Test
    void asyncPostUsesBaseUrlUriAndPayload() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        server.createContext("/api/answer", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedUri.set(exchange.getRequestURI());
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            writeJson(exchange, 200, """
                    {
                      "answer": "POST OK",
                      "score": 1.0
                    }
                    """);
        });

        AsyncRestClientService service = newService(baseUrl);

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

        assertEquals("POST", capturedMethod.get());
        assertEquals("/api/answer", capturedUri.get().toString());
        assertTrue(capturedBody.get().contains("\"question\":\"What is Spring Boot?\""));
        assertTrue(capturedBody.get().contains("\"context\":\"Spring Boot is a Java framework.\""));
        assertEquals("POST OK", result.getAnswer());
        assertEquals(1.0, result.getScore());
    }

    @Test
    void asyncPostGenericDeserialisesListResponse() {
        server.createContext("/api/answers", exchange -> writeJson(exchange, 200, """
                [
                  { "answer": "First", "score": 0.9 },
                  { "answer": "Second", "score": 0.8 }
                ]
                """));

        AsyncRestClientService service = newService(baseUrl);

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
        server.createContext("/api/slow", exchange -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            writeJson(exchange, 200, """
                    {
                      "answer": "Too slow",
                      "score": 0.1
                    }
                    """);
        });

        AsyncRestClientService service = newService(baseUrl);

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> service.asyncGet(
                                "/api/slow",
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
        server.createContext("/api/answer", exchange -> writeJson(exchange, 200, """
                {
                  "answer": "Factory OK",
                  "score": 1.0
                }
                """));

        AsyncRestClientServiceFactory factory = new AsyncRestClientServiceFactory(
                RestClient.builder(),
                restClientExecutor
        );

        AsyncRestClientService service = factory.create(baseUrl);

        AnswerResponse result = service.asyncGet(
                        "/api/answer",
                        null,
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .join();

        assertEquals("Factory OK", result.getAnswer());
        assertEquals(1.0, result.getScore());
    }

    private AsyncRestClientService newService(String serviceBaseUrl) {
        return new AsyncRestClientService(
                RestClient.builder(),
                restClientExecutor,
                serviceBaseUrl
        );
    }

    private static void writeJson(
            HttpExchange exchange,
            int statusCode,
            String json
    ) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, body.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
