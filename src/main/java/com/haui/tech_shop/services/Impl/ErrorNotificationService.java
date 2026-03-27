package com.haui.tech_shop.services.Impl;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.model.Attachment;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

@Service
public class ErrorNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationService.class);

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    @Value("${betterstack.dashboard-url}")
    private String betterstackDashboardUrl;

    private final Slack slack = Slack.getInstance();
    private final BetterstackLogService betterstackLogService;

    public ErrorNotificationService(BetterstackLogService betterstackLogService) {
        this.betterstackLogService = betterstackLogService;
    }

    public void notifyError500(HttpServletRequest request, Exception exception) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        String requestUrl = request.getRequestURL().toString();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? requestUrl + "?" + queryString : requestUrl;
        String remoteAddr = request.getRemoteAddr();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        // 1. Gửi full stack trace lên Betterstack
        betterstackLogService.sendErrorLog(errorId, method, fullUrl, remoteAddr, exception);

        // 2. Gửi thông báo tóm tắt lên Slack
        sendSlackNotification(errorId, method, fullUrl, remoteAddr, exception, timestamp);
    }

    private void sendSlackNotification(String errorId, String method, String fullUrl, String remoteAddr, Exception exception, String timestamp) {
        try {
            // Link tới Betterstack dashboard - search theo errorId
            String searchUrl = betterstackDashboardUrl;

            String message = String.format(
                    ":rotating_light: *LỖI 500 - TECH SHOP* :rotating_light:\n\n" +
                            "*Error ID:* `%s`\n" +
                            "*Thời gian:* %s\n" +
                            "*Request:* `%s %s`\n" +
                            "*IP:* %s\n" +
                            "*Exception:* `%s`\n" +
                            "*Message:* %s\n\n" +
                            ":mag: *Xem chi tiết stack trace:*\n<%s|Click vào đây>",
                    errorId, timestamp, method, fullUrl, remoteAddr,
                    exception.getClass().getSimpleName(),
                    exception.getMessage() != null ? exception.getMessage() : "N/A",
                    searchUrl
            );

            Payload payload = Payload.builder()
                    .text(message)
                    .attachments(Collections.singletonList(
                            Attachment.builder()
                                    .color("#FF0000")
                                    .fallback("Error 500 detected")
                                    .build()
                    ))
                    .build();

            slack.send(slackWebhookUrl, payload);
            log.info("Slack notification sent for error: {}", errorId);

        } catch (Exception e) {
            log.warn("Failed to send Slack notification: {}", e.getMessage());
        }
    }

    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}