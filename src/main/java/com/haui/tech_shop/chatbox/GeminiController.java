package com.haui.tech_shop.chatbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<String> chat(@RequestBody Map<String, String> payload, HttpSession session) {
        try {
            String userMessage = payload.get("message");

            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tin nhắn không được để trống");
            }

            String userId = session.getId();
            ChatResponse response = geminiService.getChatResponse(userMessage, userId);

            // Luôn trả về HTML text có link, không trả JSON
            String htmlResponse = response.getMessage();

            // Nếu có sản phẩm, thêm thông tin vào message (đã được xử lý trong service)
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(htmlResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Đã có lỗi xảy ra. Vui lòng thử lại sau.");
        }
    }

    // API để clear context khi cần
    @DeleteMapping("/clear-context")
    public ResponseEntity<String> clearContext(HttpSession session) {
        String userId = session.getId();
        geminiService.clearContext(userId);
        return ResponseEntity.ok("Đã xóa lịch sử trò chuyện");
    }
}