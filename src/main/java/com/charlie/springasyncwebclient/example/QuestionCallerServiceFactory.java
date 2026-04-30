package com.charlie.springasyncwebclient.example;

import org.springframework.stereotype.Component;

import com.charlie.springasyncwebclient.client.AsyncWebClientService;
import com.charlie.springasyncwebclient.client.AsyncWebClientServiceFactory;

@Component
public class QuestionCallerServiceFactory {

    private final AsyncWebClientServiceFactory asyncWebClientServiceFactory;

    public QuestionCallerServiceFactory(
            AsyncWebClientServiceFactory asyncWebClientServiceFactory
    ) {
        this.asyncWebClientServiceFactory = asyncWebClientServiceFactory;
    }

    public QuestionCallerService create(String baseUrl) {
        AsyncWebClientService client =
                asyncWebClientServiceFactory.create(baseUrl);

        return new QuestionCallerService(client);
    }
}