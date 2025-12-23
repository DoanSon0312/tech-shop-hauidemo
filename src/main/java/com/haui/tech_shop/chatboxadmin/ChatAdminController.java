package com.haui.tech_shop.chatboxadmin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
public class ChatAdminController {

    private final ChatAdminService chatAdminService;

    @PostMapping
    public ResponseEntity<String> chat(@RequestBody Map<String, String> payload, HttpSession session) {
        try {
            String adminMessage = payload.get("message");
            if (adminMessage == null || adminMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tin nhắn không được để trống");
            }

            String adminId = "ADMIN_" + session.getId();
            String response = chatAdminService.getAdminChatResponse(adminMessage, adminId);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi hệ thống Admin Assistant.");
        }
    }

    @DeleteMapping("/clear-context")
    public ResponseEntity<String> clearContext(HttpSession session) {
        chatAdminService.clearContext("ADMIN_" + session.getId());
        return ResponseEntity.ok("Đã làm mới phiên làm việc của Admin");
    }
}