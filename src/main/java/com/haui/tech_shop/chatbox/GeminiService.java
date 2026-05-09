package com.haui.tech_shop.chatbox;

import com.haui.tech_shop.entities.Product;
import com.haui.tech_shop.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiService {
    private final GeminiConfig config;
    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;
    private final Map<String, ConversationContext> userContexts = new HashMap<>();

    private static final String SYSTEM_INSTRUCTION =
            "Bạn là nhân viên tư vấn Tech Shop chuyên nghiệp.\n" +
                    "QUY TẮC QUAN TRỌNG:\n" +
                    "1. CHỈ nói về sản phẩm ĐÚNG được hỏi, KHÔNG nhắc sản phẩm khác\n" +
                    "2. Câu ngắn → Trả lời ngắn (1-2 câu)\n" +
                    "3. Câu tư vấn → Trả lời chi tiết, tập trung vào lợi ích\n" +
                    "4. KHÔNG dùng markdown (**, ##), chỉ dùng emoji\n" +
                    "5. Luôn kết thúc bằng câu hỏi mở";

    private static final List<String> PRODUCT_KEYWORDS = Arrays.asList(
            "laptop", "điện thoại", "máy tính", "iphone", "samsung", "dell",
            "asus", "gaming", "văn phòng", "học sinh", "sinh viên", "ram", "cpu",
            "pin", "màn hình", "card đồ họa", "giá", "tìm", "mua", "so sánh", "phone"
    );

    private static final List<String> ANAPHORA_KEYWORDS = Arrays.asList(
            "nó", "cái đó", "sản phẩm đó", "con đó", "cái này", "thằng này",
            "em đó", "thằng nào", "cái nào"
    );

    public ChatResponse getChatResponse(String userMessage, String userId) {
        ConversationContext context = userContexts.computeIfAbsent(
                userId, k -> new ConversationContext()
        );

        context.addMessage("user", userMessage);

        try {
            String intent = detectIntent(userMessage, context);
            context.setUserIntent(intent);

            ChatResponse response;

            switch (intent) {
                case "anaphora_detail":
                    response = handleAnaphoraQuestion(userMessage, context);
                    break;
                case "extreme_price":
                    response = handleExtremePrice(userMessage, context);
                    break;
                case "product_search":
                    response = handleProductSearch(userMessage, context);
                    break;
                case "product_compare":
                    response = handleProductCompare(userMessage, context);
                    break;
                case "price_inquiry":
                    response = handlePriceInquiry(userMessage, context);
                    break;
                case "recommendation":
                    response = handleRecommendation(userMessage, context);
                    break;
                default:
                    response = handleGeneralQuestion(userMessage, context);
            }

            context.addMessage("assistant", response.getMessage());
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse(
                    "Xin lỗi, em gặp chút vấn đề. Anh/Chị có thể hỏi lại được không ạ?",
                    null
            );
        }
    }

    private String detectIntent(String message, ConversationContext context) {
        String lowerMessage = message.toLowerCase().trim();

        if (ANAPHORA_KEYWORDS.stream().anyMatch(lowerMessage::contains) &&
                context.getLastDiscussedProduct() != null) {
            return "anaphora_detail";
        }

        if (lowerMessage.matches(".*(đắt nhất|rẻ nhất|cao nhất|thấp nhất|max price|min price).*")) {
            return "extreme_price";
        }

        if (lowerMessage.matches(".*(so sánh|khác nhau|hơn|tốt hơn|giống|vs|với).*")) {
            return "product_compare";
        }

        if (lowerMessage.matches(".*(giá|bao nhiêu|giá bao nhiêu|giá cả|chi phí).*")) {
            return "price_inquiry";
        }

        // CẬP NHẬT: Thêm các pattern hỏi mua hàng để nhận diện là Tư vấn (Recommendation)
        if (lowerMessage.matches(".*(tư vấn|gợi ý|nên mua|nên chọn|đề xuất|recommend|máy nào|con nào|loại nào).*")) {
            return "recommendation";
        }

        if (PRODUCT_KEYWORDS.stream().anyMatch(lowerMessage::contains) ||
                lowerMessage.matches(".*(tìm|mua|có|xem).*")) {
            return "product_search";
        }

        return "general";
    }

    // ==================== XỬ LÝ ĐẮT NHẤT / RẺ NHẤT ====================
    private ChatResponse handleExtremePrice(String message, ConversationContext context) {
        String lowerMessage = message.toLowerCase();
        boolean isMostExpensive = lowerMessage.contains("đắt") || lowerMessage.contains("cao");
        String category = extractCategory(message);

        List<Product> products = productRepository.findByActiveTrue();

        if (category != null) {
            products = products.stream()
                    .filter(p -> p.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        if (products.isEmpty()) {
            return ChatResponse.fromProducts("Hiện tại bên em chưa có sản phẩm nào thuộc danh mục này ạ.", null);
        }

        Comparator<Product> priceComparator = Comparator.comparing(Product::getPrice);
        if (isMostExpensive) {
            priceComparator = priceComparator.reversed();
        }

        Product targetProduct = products.stream()
                .sorted(priceComparator)
                .findFirst()
                .orElse(null);

        if (targetProduct == null) return ChatResponse.fromProducts("Dạ em không tìm thấy dữ liệu ạ.", null);

        context.setLastDiscussedProduct(targetProduct);
        context.setLastSearchResults(Collections.singletonList(targetProduct));

        String prompt = String.format(
                "%s\n\nKhách hỏi: '%s'\nSản phẩm tìm được: %s\nGiá: %s\n\n" +
                        "Nhiệm vụ: Giới thiệu đây là sản phẩm %s nhất hiện có. Nêu ngắn gọn điểm nổi bật của nó.",
                SYSTEM_INSTRUCTION,
                message,
                targetProduct.getName(),
                formatPrice(targetProduct.getPrice()),
                isMostExpensive ? "cao cấp/đắt tiền" : "giá rẻ/tiết kiệm"
        );

        String aiResponse = callGeminiAPIWithHistory(prompt, context);
        String finalResponse = addProductLinks(aiResponse) + buildProductLinksHTML(Collections.singletonList(targetProduct));

        return ChatResponse.fromProducts(finalResponse, Collections.singletonList(targetProduct));
    }

    // ==================== XỬ LÝ ANAPHORA ====================
    private ChatResponse handleAnaphoraQuestion(String message, ConversationContext context) {
        Product lastProduct = context.getLastDiscussedProduct();
        if (lastProduct == null) {
            return ChatResponse.fromProducts(
                    "Em chưa hiểu Anh/Chị đang hỏi về sản phẩm nào ạ. Anh/Chị có thể nói rõ hơn được không?",
                    null
            );
        }

        String lowerMessage = message.toLowerCase();
        StringBuilder response = new StringBuilder();
        response.append("Dạ, về ").append(createProductLink(lastProduct)).append(":\n\n");

        boolean detailedAsked = false;
        if (lowerMessage.matches(".*(ram|bộ nhớ).*")) {
            response.append("💾 RAM: ").append(orNA(lastProduct.getRam())).append("\n");
            detailedAsked = true;
        }
        if (lowerMessage.matches(".*(cpu|chip|bộ xử lý|vi xử lý).*")) {
            response.append("⚡ CPU: ").append(orNA(lastProduct.getCpu())).append("\n");
            detailedAsked = true;
        }
        if (lowerMessage.matches(".*(pin|battery|dung lượng pin).*")) {
            response.append("🔋 Pin: ").append(orNA(lastProduct.getBattery())).append("\n");
            detailedAsked = true;
        }
        if (lowerMessage.matches(".*(màn hình|monitor|display|screen).*")) {
            response.append("🖥️ Màn hình: ").append(orNA(lastProduct.getMonitor())).append("\n");
            detailedAsked = true;
        }
        if (lowerMessage.matches(".*(card|đồ họa|gpu|vga).*")) {
            response.append("🎮 Card đồ họa: ").append(orNA(lastProduct.getGraphicCard())).append("\n");
            detailedAsked = true;
        }
        if (lowerMessage.matches(".*(giá|bao nhiêu|tiền).*")) {
            response.append("💰 Giá: ").append(formatPrice(lastProduct.getPrice())).append("\n");
            detailedAsked = true;
        }
        if (lowerMessage.matches(".*(bảo hành|warranty).*")) {
            response.append("🛡️ Bảo hành: ").append(lastProduct.getWarranty()).append("\n");
            detailedAsked = true;
        }

        if (!detailedAsked) {
            String prompt = String.format(
                    "%s\n\nSản phẩm:\n%s\n\nCâu hỏi: %s\n\nTrả lời NGẮN GỌN đúng câu hỏi. Kết thúc: 'Anh/Chị muốn biết thêm gì về %s không ạ?'",
                    SYSTEM_INSTRUCTION,
                    buildProductInfo(lastProduct),
                    message,
                    lastProduct.getName()
            );
            String aiResponse = callGeminiAPIWithHistory(prompt, context);
            return ChatResponse.fromProducts(addProductLinks(aiResponse), Collections.singletonList(lastProduct));
        }

        response.append("\n💡 Anh/Chị muốn biết thêm thông tin gì về ").append(lastProduct.getName()).append(" không ạ?");
        return ChatResponse.fromProducts(response.toString(), Collections.singletonList(lastProduct));
    }

    // ==================== XỬ LÝ TÌM KIẾM SẢN PHẨM ====================
    private ChatResponse handleProductSearch(String message, ConversationContext context) {
        String keyword = extractSearchKeyword(message);
        String category = extractCategory(message);
        context.setLastSearchKeyword(keyword);

        List<Product> products = searchProducts(keyword, category, message);

        // CẬP NHẬT: Logic thông minh hơn
        // Nếu không tìm thấy sản phẩm nào theo từ khóa, hãy kiểm tra xem ý định của khách là gì.
        // Ví dụ: khách tìm "chs game" (typo) -> không ra sp -> nhưng intent là "gaming".
        // Lúc này chuyển sang handleRecommendation để tư vấn chứ không xin lỗi.
        if (products.isEmpty()) {
            String detectedIntent = analyzeUserIntent(message);
            if (!detectedIntent.equals("general")) {
                // Tự động chuyển hướng sang Tư vấn
                return handleRecommendation(message, context);
            }
            // Nếu không có ý định rõ ràng mới báo lỗi
            return handleNoProductFound(keyword, context, category);
        }

        List<Product> limitedProducts = products.stream().limit(3).collect(Collectors.toList());

        context.setLastSearchResults(limitedProducts);
        context.setLastDiscussedProduct(limitedProducts.get(0));

        String prompt = String.format(
                "%s\n\nKhách tìm: '%s'\nSản phẩm tìm thấy:\n%s\n\n" +
                        "Nhiệm vụ: Giới thiệu chung về các sản phẩm này trong 1-2 câu. Mời khách xem chi tiết bên dưới.",
                SYSTEM_INSTRUCTION,
                keyword,
                buildProductListInfo(limitedProducts)
        );

        String aiResponse = callGeminiAPIWithHistory(prompt, context);
        String finalResponse = addProductLinks(aiResponse) + buildProductLinksHTML(limitedProducts);

        return ChatResponse.fromProducts(finalResponse, limitedProducts);
    }

    // ==================== XỬ LÝ SO SÁNH ====================
    private ChatResponse handleProductCompare(String message, ConversationContext context) {
        List<Product> productsToCompare = extractProductsFromMessage(message);

        if (productsToCompare.size() < 2) {
            if (context.getLastSearchResults() != null && context.getLastSearchResults().size() >= 2) {
                productsToCompare = context.getLastSearchResults().subList(0, 2);
            } else {
                return ChatResponse.fromProducts(
                        "Em cần tên 2 sản phẩm để so sánh ạ. Ví dụ: 'So sánh Asus TUF và MSI Titan' 😊",
                        null
                );
            }
        }

        Product p1 = productsToCompare.get(0);
        Product p2 = productsToCompare.get(1);

        String prompt = String.format(
                "%s\n\nSo sánh 2 sản phẩm:\n\nSẢN PHẨM 1:\n%s\n\nSẢN PHẨM 2:\n%s\n\n" +
                        "Yêu cầu: NGẮN GỌN, nêu điểm mạnh từng con. Kết thúc: 'Anh/Chị quan tâm yếu tố nào nhất ạ?'",
                SYSTEM_INSTRUCTION,
                buildProductInfo(p1),
                buildProductInfo(p2)
        );

        String aiResponse = callGeminiAPIWithHistory(prompt, context);
        String finalResponse = addProductLinks(aiResponse);

        return ChatResponse.fromProducts(finalResponse, Arrays.asList(p1, p2));
    }

    // ==================== XỬ LÝ HỎI GIÁ ====================
    private ChatResponse handlePriceInquiry(String message, ConversationContext context) {
        Product targetProduct = findExactProductInMessage(message);

        if (targetProduct == null && context.getLastDiscussedProduct() != null) {
            targetProduct = context.getLastDiscussedProduct();
        }

        if (targetProduct == null) {
            return ChatResponse.fromProducts(
                    "Em chưa rõ Anh/Chị hỏi giá sản phẩm nào ạ. Anh/Chị cho em tên cụ thể nhé! 😊",
                    null
            );
        }

        context.setLastDiscussedProduct(targetProduct);

        String response = String.format(
                "Giá của %s là %s ạ.\n\nAnh/Chị muốn biết thêm gì về %s không ạ?",
                createProductLink(targetProduct),
                formatPrice(targetProduct.getPrice()),
                targetProduct.getName()
        );

        return ChatResponse.fromProducts(response, Collections.singletonList(targetProduct));
    }

    // ==================== XỬ LÝ TƯ VẤN ====================
    private ChatResponse handleRecommendation(String message, ConversationContext context) {
        String intent = analyzeUserIntent(message);
        String category = extractCategory(message);
        BigDecimal maxPrice = extractBudget(message);

        List<Product> recommendedProducts = findProductsByIntent(intent, message, category);

        if (maxPrice != null) {
            recommendedProducts = recommendedProducts.stream()
                    .filter(p -> p.getPrice().compareTo(maxPrice) <= 0)
                    .collect(Collectors.toList());
        }

        if (recommendedProducts.isEmpty()) {
            recommendedProducts = productRepository.findByActiveTrue().stream()
                    .limit(3)
                    .collect(Collectors.toList());
        }

        // Prompt được sửa lại để AI trả lời tự nhiên hơn khi chuyển từ Search sang
        String prompt = String.format(
                "%s\n\nKhách đang quan tâm: '%s'\nNhu cầu phát hiện: %s\n\nSản phẩm phù hợp nhất trong kho:\n%s\n\n" +
                        "Nhiệm vụ: Đừng xin lỗi. Hãy chào khách và giới thiệu ngay các sản phẩm phù hợp này. Nêu lý do tại sao nó hợp với nhu cầu (ví dụ: chơi game mượt, pin trâu...).",
                SYSTEM_INSTRUCTION,
                message,
                intent,
                buildProductListInfo(recommendedProducts)
        );

        String aiResponse = callGeminiAPIWithHistory(prompt, context);
        String finalResponse = addProductLinks(aiResponse) + buildProductLinksHTML(recommendedProducts);

        return ChatResponse.fromProducts(finalResponse, recommendedProducts);
    }

    // ==================== XỬ LÝ CÂU HỎI CHUNG ====================
    private ChatResponse handleGeneralQuestion(String message, ConversationContext context) {
        String prompt = String.format(
                "%s\n\nKhách hỏi: %s\n\nTrả lời NGẮN GỌN (2-3 câu).",
                SYSTEM_INSTRUCTION,
                message
        );
        String aiResponse = callGeminiAPIWithHistory(prompt, context);
        return ChatResponse.fromProducts(aiResponse, null);
    }

    // ==================== XỬ LÝ KHÔNG TÌM THẤY ====================
    private ChatResponse handleNoProductFound(String keyword, ConversationContext context, String category) {
        // Fallback: Tìm sản phẩm gợi ý ngẫu nhiên hoặc theo category
        List<Product> recommendedProducts = productRepository.findByActiveTrue();
        if (category != null) {
            recommendedProducts = recommendedProducts.stream()
                    .filter(p -> p.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        if (recommendedProducts.size() > 3) {
            recommendedProducts = recommendedProducts.subList(0, 3);
        }

        String prompt = String.format(
                "%s\n\nKhách tìm: '%s' -> KHÔNG CÓ trong kho.\nSản phẩm khác đang có:\n%s\n\n" +
                        "Nhiệm vụ: Xin lỗi khách nhẹ nhàng và gợi ý khách xem thử các mẫu này.",
                SYSTEM_INSTRUCTION,
                keyword,
                buildProductListInfo(recommendedProducts)
        );

        String aiResponse = callGeminiAPIWithHistory(prompt, context);
        String finalResponse = addProductLinks(aiResponse) + buildProductLinksHTML(recommendedProducts);

        return ChatResponse.fromProducts(finalResponse, recommendedProducts);
    }

    // ============ CORE METHODS ============
    private String callGeminiAPIWithHistory(String currentPrompt, ConversationContext context) {
        String url = config.getUrl() + "?key=" + config.getApiKey();

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))));
        contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", "Dạ em hiểu rồi ạ!"))));

        List<ConversationContext.Message> recent = context.getConversationHistory();
        int startIdx = Math.max(0, recent.size() - 4);

        for (int i = startIdx; i < recent.size(); i++) {
            ConversationContext.Message msg = recent.get(i);
            String role = msg.getRole().equals("user") ? "user" : "model";
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", msg.getContent()))));
        }

        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", currentPrompt))));

        Map<String, Object> body = Map.of(
                "contents", contents,
                "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 1024)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null) {
                var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    var parts = (List<Map<String, Object>>) content.get("parts");
                    return cleanMarkdown((String) parts.get(0).get("text"));
                }
            }
            return "Xin lỗi, em không thể trả lời lúc này ạ.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Có lỗi xảy ra. Anh/Chị thử lại sau nhé! 😢";
        }
    }

    // ============ PRODUCT HELPER METHODS ============
    private Product findExactProductInMessage(String message) {
        List<Product> allProducts = productRepository.findByActiveTrue();
        String lower = message.toLowerCase();
        return allProducts.stream()
                .filter(p -> lower.contains(p.getName().toLowerCase()))
                .max(Comparator.comparingInt(p -> p.getName().length()))
                .orElse(null);
    }

    private List<Product> extractProductsFromMessage(String message) {
        List<Product> allProducts = productRepository.findByActiveTrue();
        List<Product> found = new ArrayList<>();
        String lower = message.toLowerCase();
        for (Product p : allProducts) {
            if (lower.contains(p.getName().toLowerCase())) {
                found.add(p);
                if (found.size() == 2) break;
            }
        }
        return found;
    }

    private String extractSearchKeyword(String message) {
        return message.toLowerCase()
                .replaceAll("\\b(tìm kiếm|tìm|mua|xem|có|bán|cho tôi|giúp tôi|em muốn|tôi cần|cho em)\\b", "")
                .replaceAll("[?!.,]", "")
                .trim();
    }

    private List<Product> searchProducts(String keyword, String category, String fullMessage) {
        List<Product> results = new ArrayList<>();
        String lower = keyword.toLowerCase();

        results.addAll(productRepository.findByNameContainingIgnoreCase(keyword));
        if (results.isEmpty()) {
            results.addAll(productRepository.findByActiveTrue().stream()
                    .filter(p -> p.getBrand().getName().toLowerCase().contains(lower))
                    .collect(Collectors.toList()));
        }
        if (results.isEmpty()) {
            results.addAll(productRepository.findByDescriptionContainingIgnoreCase(keyword));
        }
        if (category != null && !results.isEmpty()) {
            results = results.stream()
                    .filter(p -> p.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        BigDecimal minPrice = extractMinPrice(fullMessage);
        BigDecimal maxPrice = extractMaxPrice(fullMessage);
        if (minPrice != null || maxPrice != null) {
            results = results.stream()
                    .filter(p -> (minPrice == null || p.getPrice().compareTo(minPrice) >= 0) &&
                            (maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0))
                    .collect(Collectors.toList());
        }
        return results.stream().filter(Product::isActive).distinct().collect(Collectors.toList());
    }

    private String analyzeUserIntent(String message) {
        String lower = message.toLowerCase();
        // Thêm "chs" vào đây để bắt typo
        if (lower.matches(".*(gaming|game|chs game|chơi game|đồ họa|render|rtx|gtx).*")) return "gaming";
        if (lower.matches(".*(\\d+)\\s*(triệu|tr).*đến.*(\\d+).*") || lower.matches(".*(từ|dưới|trên).*\\d+.*(triệu|tr).*")) return "price_range";
        if (lower.matches(".*(văn phòng|office|học tập|sinh viên|nhẹ|mỏng).*")) return "office";
        if (lower.matches(".*(cao cấp|premium|flagship|đắt|xịn).*")) return "premium";
        if (lower.matches(".*(rẻ|giá tốt|phải chăng|tiết kiệm|budget).*")) return "budget";
        return "general";
    }

    private List<Product> findProductsByIntent(String intent, String message, String category) {
        List<Product> allProducts = productRepository.findByActiveTrue();
        if (category != null) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        BigDecimal minPrice = extractMinPrice(message);
        BigDecimal maxPrice = extractMaxPrice(message);

        Comparator<Product> priceAsc = Comparator.comparing(Product::getPrice);
        Comparator<Product> priceDesc = Comparator.comparing(Product::getPrice).reversed();

        switch (intent) {
            case "gaming":
                return allProducts.stream()
                        .filter(p -> {
                            String t = (p.getName() + " " + p.getDescription() + " " + orNA(p.getGraphicCard())).toLowerCase();
                            return t.contains("gaming") || t.contains("rtx") || t.contains("gtx") || t.contains("game");
                        })
                        .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                        .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                        .sorted(priceDesc).limit(5).collect(Collectors.toList());
            case "office":
                return allProducts.stream()
                        .filter(p -> {
                            String t = (p.getName() + " " + p.getDescription()).toLowerCase();
                            return t.contains("văn phòng") || t.contains("office") || t.contains("business");
                        })
                        .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                        .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                        .sorted(priceAsc).limit(5).collect(Collectors.toList());
            case "price_range":
                return allProducts.stream()
                        .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                        .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                        .sorted(priceAsc).limit(5).collect(Collectors.toList());
            case "premium":
                return allProducts.stream().sorted(priceDesc).limit(5).collect(Collectors.toList());
            case "budget":
                return allProducts.stream().sorted(priceAsc).limit(5).collect(Collectors.toList());
            default:
                return allProducts.stream().limit(5).collect(Collectors.toList());
        }
    }

    private BigDecimal extractMinPrice(String message) {
        Pattern p = Pattern.compile("(từ|trên)\\s*(\\d+)\\s*(triệu|tr)");
        var m = p.matcher(message.toLowerCase());
        return m.find() ? new BigDecimal(m.group(2)).multiply(new BigDecimal("1000000")) : null;
    }

    private BigDecimal extractMaxPrice(String message) {
        Pattern p = Pattern.compile("(dưới|đến)\\s*(\\d+)\\s*(triệu|tr)");
        var m = p.matcher(message.toLowerCase());
        return m.find() ? new BigDecimal(m.group(2)).multiply(new BigDecimal("1000000")) : null;
    }

    private BigDecimal extractBudget(String message) {
        Pattern p = Pattern.compile("(\\d+)\\s*(triệu|tr|million)");
        var m = p.matcher(message.toLowerCase());
        return m.find() ? new BigDecimal(m.group(1)).multiply(new BigDecimal("1000000")) : null;
    }

    private String extractCategory(String message) {
        String l = message.toLowerCase();
        if (l.matches(".*(điện thoại|phone|smartphone|đtdd|dt).*")) return "Phone";
        if (l.matches(".*(laptop|máy tính|macbook|notebook).*")) return "Computer";
        if (l.matches(".*(phụ kiện|accessory|tai nghe|sạc|cáp).*")) return "Accessory";
        return null;
    }

    private String addProductLinks(String text) {
        List<Product> allProducts = productRepository.findByActiveTrue();
        allProducts.sort((p1, p2) -> Integer.compare(p2.getName().length(), p1.getName().length()));
        for (Product p : allProducts) {
            String link = createProductLink(p);
            text = text.replaceAll("(?i)" + Pattern.quote(p.getName()), link);
        }
        return text;
    }

    private String createProductLink(Product p) {
        return String.format("<a href='/user/products/product-detail/%d' style='color: #667eea; font-weight: 600; text-decoration: none;'>%s</a>", p.getId(), p.getName());
    }

    private String buildProductInfo(Product p) {
        return String.format("Tên: %s\nGiá: %s\nCPU: %s\nRAM: %s\nPin: %s\nMàn hình: %s\nCard: %s\nBảo hành: %s\nMô tả: %s",
                p.getName(), formatPrice(p.getPrice()), orNA(p.getCpu()), orNA(p.getRam()), orNA(p.getBattery()),
                orNA(p.getMonitor()), orNA(p.getGraphicCard()), p.getWarranty(), orNA(p.getDescription()));
    }

    private String buildProductListInfo(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append(String.format("%d. %s - %s | CPU: %s | RAM: %s | Giá: %s\n",
                    i + 1, p.getName(), orNA(p.getDescription()), orNA(p.getCpu()), orNA(p.getRam()), formatPrice(p.getPrice())));
        }
        return sb.toString();
    }

    private String buildProductLinksHTML(List<Product> products) {
        if (products == null || products.size() <= 1) return "";
        StringBuilder sb = new StringBuilder("\n✨ <b>Sản phẩm tham khảo:</b>\n");
        for (Product p : products) {
            sb.append(String.format(
                    "• %s - <span style='color: #e53e3e; font-weight: bold;'>%s</span>\n",
                    createProductLink(p),
                    formatPrice(p.getPrice())
            ));
        }
        return sb.toString();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "Liên hệ";
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(price) + "đ";
    }

    private String cleanMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("__([^_]+)__", "$1")
                .replaceAll("_([^_]+)_", "$1")
                .replaceAll("~~([^~]+)~~", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("#{1,6}\\s*", "")
                .trim();
    }

    private String orNA(String value) {
        return value != null && !value.isEmpty() ? value : "N/A";
    }

    public void clearContext(String userId) {
        userContexts.remove(userId);
    }
}