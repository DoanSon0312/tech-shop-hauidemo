package com.haui.tech_shop.chatbox;

import com.haui.tech_shop.entities.Product;
import lombok.Data;
import java.util.*;

@Data
public class ConversationContext {
    private List<Message> conversationHistory;
    private Product lastDiscussedProduct;
    private String lastSearchKeyword;
    private List<Product> lastSearchResults;
    private String userIntent; // "search", "compare", "detail", "general"

    public ConversationContext() {
        this.conversationHistory = new ArrayList<>();
    }

    public void addMessage(String role, String content) {
        conversationHistory.add(new Message(role, content));
        if (conversationHistory.size() > 10) { // Giữ 10 tin nhắn gần nhất
            conversationHistory.remove(0);
        }
    }

    public String getConversationHistoryText() {
        StringBuilder sb = new StringBuilder();
        for (Message msg : conversationHistory) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    @Data
    public static class Message {
        private String role; // "user" or "assistant"
        private String content;
        private long timestamp;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
}