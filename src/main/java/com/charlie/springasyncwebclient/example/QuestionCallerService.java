package com.charlie.springasyncwebclient.example;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.charlie.springasyncwebclient.client.AsyncWebClientService;
import com.charlie.springasyncwebclient.dto.AnswerResponse;
import com.charlie.springasyncwebclient.dto.QuestionRequest;

public class QuestionCallerService {

    private final AsyncWebClientService asyncWebClientService;

    public QuestionCallerService(AsyncWebClientService asyncWebClientService) {
        this.asyncWebClientService = asyncWebClientService;
    }

    public CompletableFuture<AnswerResponse> askQuestionUsingWebClient() {

        QuestionRequest request = new QuestionRequest(
                "What is Spring Boot?",
                "Spring Boot is a Java framework for building web applications."
        );

        return asyncWebClientService.asyncPost(
                        "/api/answer",
                        request,
                        AnswerResponse.class,
                        Duration.ofSeconds(10)
                )
                .thenApply(answerResponse -> {
                    System.out.println("WebClient call succeeded");
                    System.out.println("Answer: " + answerResponse.getAnswer());
                    System.out.println("Score: " + answerResponse.getScore());

                    return answerResponse;
                })
                .exceptionally(error -> {
                    System.err.println("WebClient call failed");
                    System.err.println(error.getMessage());

                    AnswerResponse fallback = new AnswerResponse();
                    fallback.setAnswer("No answer available because the remote service failed.");
                    fallback.setScore(0.0);

                    return fallback;
                });
    }
}