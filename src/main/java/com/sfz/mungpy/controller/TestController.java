package com.sfz.mungpy.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    private final String secretKey;

    public TestController(@Value("${openaiapi.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @GetMapping
    public String test() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();

        return restClient.get()
                .retrieve()
                .body(String.class);
    }

    @GetMapping("/ai")
    public String gpt() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/")
                .build();

        String json = """
                {
                    "model": "gpt-3.5-turbo",
                    "messages": [
                      {
                        "role": "user",
                        "content": "안녕?"
                      }
                    ]
                }
                """;

        return restClient.post()
                .uri("v1/chat/completions")
                .header("Authorization", secretKey)
                .header("Content-Type", "application/json")
                .body(json)
                .retrieve()
                .toEntity(String.class)
                .getBody();
    }
}
