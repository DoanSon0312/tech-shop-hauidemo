package com.haui.tech_shop.services.Impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Service
public class BetterstackLogService {

    @Value("${betterstack.source-token}")
    private String sourceToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendErrorLog(String errorId, String method, String url,
                             String remoteAddr, Exception exception) {
        try {
            String stackTrace = getStackTrace(exception);

            String jsonPayload = String.format(
                    "{" +
                            "\"dt\":\"%s\"," +
                            "\"level\":\"error\"," +
                            "\"message\":\"[ERROR-500][%s] %s %s\"," +
                            "\"errorId\":\"%s\"," +
                            "\"method\":\"%s\"," +
                            "\"url\":\"%s\"," +
                            "\"remoteAddr\":\"%s\"," +
                            "\"exceptionType\":\"%s\"," +
                            "\"exceptionMessage\":\"%s\"," +
                            "\"stackTrace\":\"%s\"" +
                            "}",
                    Instant.now().toString(),
                    errorId, method, escapeJson(url),
                    errorId,
                    method,
                    escapeJson(url),
                    remoteAddr,
                    exception.getClass().getName(),
                    escapeJson(exception.getMessage() != null ? exception.getMessage() : "N/A"),
                    escapeJson(stackTrace)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://in.logs.betterstack.com"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + sourceToken)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Betterstack response: " + response.statusCode());

        } catch (Exception e) {
            System.err.println("Failed to send log to Betterstack: " + e.getMessage());
        }
    }

    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
