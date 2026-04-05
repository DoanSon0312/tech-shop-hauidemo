package com.haui.tech_shop.services.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SepayService {

    @Value("${sepay.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kiểm tra xem đã có giao dịch nào khớp với nội dung chuyển khoản và số tiền chưa.
     * SePay API: GET https://my.sepay.vn/userapi/transactions/list
     * Params: account_number, limit, content (nội dung chuyển khoản)
     */
    public boolean checkPaymentReceived(String transferContent, long expectedAmount) {
        try {
            String url = "https://my.sepay.vn/userapi/transactions/list"
                    + "?limit=10"
                    + "&content=" + transferContent;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode transactions = root.get("transactions");

                if (transactions != null && transactions.isArray()) {
                    for (JsonNode tx : transactions) {
                        long txAmount = tx.get("amount_in").asLong(0);
                        String txContent = tx.has("transaction_content")
                                ? tx.get("transaction_content").asText("") : "";

                        // So khớp: nội dung chứa mã đơn hàng VÀ số tiền >= expected
                        if (txAmount >= expectedAmount
                                && txContent.toUpperCase().contains(transferContent.toUpperCase())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}