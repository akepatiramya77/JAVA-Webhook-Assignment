package com.example.webhookapp;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Component
class StartupRunner implements ApplicationRunner {

    private final RestTemplate restTemplate;

    @Value("${app.generateUrl}")
    private String generateUrl;

    @Value("${app.defaultTestUrl}")
    private String defaultTestUrl;

    @Value("${app.name}")
    private String name;

    @Value("${app.regNo}")
    private String regNo;

    @Value("${app.email}")
    private String email;

    @Value("${app.finalQueryFile:finalQuery.sql}")
    private String finalQueryFile;

    public StartupRunner(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Starting webhook process...");

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("regNo", regNo);
        body.put("email", email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // 1️⃣ Call generateWebhook API
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                generateUrl, HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            System.err.println("❌ Failed to generate webhook!");
            return;
        }

        Map<String, Object> respBody = response.getBody();
        System.out.println("✅ Webhook generated: " + respBody);

        String webhookUrl = (String) respBody.getOrDefault("webhook", null);
        String accessToken = (String) respBody.getOrDefault("accessToken", null);

        if (webhookUrl == null || accessToken == null) {
            System.err.println("Missing webhook or accessToken");
            return;
        }

        // 2️⃣ Read your SQL query from file
        String finalQuery = readFinalQuery(finalQueryFile);
        if (finalQuery == null || finalQuery.isEmpty()) {
            System.err.println("❌ Put your SQL query inside finalQuery.sql");
            return;
        }

        // 3️⃣ Submit finalQuery
        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        submitHeaders.set("Authorization", accessToken);

        Map<String, String> submitBody = new HashMap<>();
        submitBody.put("finalQuery", finalQuery);

        HttpEntity<Map<String, String>> submitRequest = new HttpEntity<>(submitBody, submitHeaders);
        ResponseEntity<String> submitResponse = restTemplate.exchange(webhookUrl, HttpMethod.POST, submitRequest, String.class);

        System.out.println("🚀 Submission done! Status: " + submitResponse.getStatusCode());
        System.out.println("Response: " + submitResponse.getBody());
    }

    private String readFinalQuery(String pathStr) {
        try {
            Path path = Path.of(pathStr);
            if (!Files.exists(path)) {
                return null;
            }
            return Files.readString(path).trim();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
