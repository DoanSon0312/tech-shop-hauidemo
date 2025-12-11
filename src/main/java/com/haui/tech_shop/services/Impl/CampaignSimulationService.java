package com.haui.tech_shop.services.Impl;

import com.haui.tech_shop.entities.Product;
import com.haui.tech_shop.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignSimulationService {

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public Map<String, Object> simulateCampaign(String campaignDescription) {
        try {
            // 1. Phân tích mô tả thông minh hơn
            CampaignInfo campaignInfo = parseCampaignDescription(campaignDescription);

            // 2. Tìm sản phẩm dựa trên keyword chính và phụ (song ngữ)
            List<Product> targetProducts = getTargetProducts(campaignInfo);

            if (targetProducts.isEmpty()) {
                return createErrorResponse("Không tìm thấy sản phẩm phù hợp với từ khóa: " + campaignInfo.getTargetKeyword()
                        + ". Hãy thử mô tả rõ hơn (ví dụ: laptop, điện thoại, chuột...)");
            }

            ProductMetrics metrics = calculateCurrentMetrics(targetProducts);

            // 3. Gọi AI với Prompt nâng cao
            AISimulationResult aiResult = callGeminiAI(campaignInfo, metrics, targetProducts);

            return buildSimulationResponse(campaignInfo, metrics, aiResult, targetProducts);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // === PHẦN 1: PARSER THÔNG MINH ===
    private CampaignInfo parseCampaignDescription(String description) {
        CampaignInfo info = new CampaignInfo();
        info.setOriginalDescription(description);
        String lowerDesc = description.toLowerCase();

        // 1. Bóc tách phần trăm giảm giá
        Pattern percentPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
        Matcher percentMatcher = percentPattern.matcher(description);
        if (percentMatcher.find()) {
            info.setDiscountPercentage((int) Double.parseDouble(percentMatcher.group(1)));
        } else {
            info.setDiscountPercentage(0);
        }

        // 2. Nhận diện Category thông minh (Mapping từ đồng nghĩa)
        if (containsAny(lowerDesc, "laptop", "máy tính xách tay", "macbook", "notebook", "pc")) {
            info.setTargetKeyword("laptop");       // Keyword tìm trong DB (ưu tiên tiếng Anh nếu DB lưu tiếng Anh)
            info.setSecondaryKeyword("máy tính");  // Keyword phụ tiếng Việt
            info.setTargetCategory("Laptop & Máy tính");
        }
        else if (containsAny(lowerDesc, "điện thoại", "phone", "smartphone", "mobile", "iphone", "samsung", "android", "dế")) {
            info.setTargetKeyword("phone");
            info.setSecondaryKeyword("điện thoại");
            info.setTargetCategory("Điện thoại thông minh");
        }
        else if (containsAny(lowerDesc, "tablet", "máy tính bảng", "ipad", "tab")) {
            info.setTargetKeyword("tablet");
            info.setSecondaryKeyword("máy tính bảng");
            info.setTargetCategory("Máy tính bảng");
        }
        else if (containsAny(lowerDesc, "phụ kiện", "accessory", "chuột", "mouse", "phím", "keyboard", "tai nghe", "headphone", "loa")) {
            info.setTargetKeyword("accessory");
            info.setSecondaryKeyword("phụ kiện");
            info.setTargetCategory("Phụ kiện công nghệ");
        }
        else if (lowerDesc.contains("gaming")) {
            info.setTargetKeyword("gaming");
            info.setSecondaryKeyword("chơi game");
            info.setTargetCategory("Gaming Gear");
        }
        else {
            info.setTargetKeyword("all");
            info.setTargetCategory("Tất cả sản phẩm");
        }

        return info;
    }

    // Hàm hỗ trợ kiểm tra nhiều từ khóa cùng lúc
    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    // === PHẦN 2: TÌM SẢN PHẨM LINH HOẠT ===
    private List<Product> getTargetProducts(CampaignInfo campaignInfo) {
        String key1 = campaignInfo.getTargetKeyword().toLowerCase();

        if ("all".equalsIgnoreCase(key1)) {
            return productRepository.findByActiveTrue();
        }

        // Xử lý đặc biệt cho phụ kiện: Tìm gộp cả chuột, phím, tai nghe
        else if ("accessory".equals(key1) || "phụ kiện".equals(key1)) {
            List<Product> products = new ArrayList<>();
            products.addAll(productRepository.findByFlexibleKeyword("mouse"));
            products.addAll(productRepository.findByFlexibleKeyword("chuột"));
            products.addAll(productRepository.findByFlexibleKeyword("keyboard"));
            products.addAll(productRepository.findByFlexibleKeyword("bàn phím"));
            products.addAll(productRepository.findByFlexibleKeyword("headphone"));
            products.addAll(productRepository.findByFlexibleKeyword("tai nghe"));
            // Lọc trùng lặp
            return products.stream().distinct().collect(Collectors.toList());
        }

        // Tìm kiếm song ngữ (Ví dụ: tìm cả 'phone' và 'điện thoại')
        else {
            String key2 = campaignInfo.getSecondaryKeyword();
            if (key2 != null && !key2.isEmpty()) {
                return productRepository.findByDualKeyword(key1, key2);
            } else {
                return productRepository.findByFlexibleKeyword(key1);
            }
        }
    }

    private ProductMetrics calculateCurrentMetrics(List<Product> products) {
        ProductMetrics metrics = new ProductMetrics();
        BigDecimal totalValue = BigDecimal.ZERO;
        int totalStock = 0;

        for (Product product : products) {
            BigDecimal productValue = product.getPrice().multiply(new BigDecimal(product.getStockQuantity()));
            totalValue = totalValue.add(productValue);
            totalStock += product.getStockQuantity();
        }

        metrics.setTotalProducts(products.size());
        metrics.setTotalStock(totalStock);
        metrics.setTotalInventoryValue(totalValue);
        metrics.setAveragePrice(totalStock > 0 ? totalValue.divide(new BigDecimal(totalStock), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return metrics;
    }

    private AISimulationResult callGeminiAI(CampaignInfo campaignInfo, ProductMetrics metrics, List<Product> products) {
        String prompt = buildSmartGeminiPrompt(campaignInfo, metrics, products);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("text", prompt);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(contentPart));
            requestBody.put("contents", Collections.singletonList(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = geminiApiUrl + "?key=" + geminiApiKey;

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return parseGeminiResponse(response.getBody(), campaignInfo, metrics);

        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultPrediction(campaignInfo, metrics);
        }
    }

    // === PHẦN 3: PROMPT AI CHI TIẾT (DOMAIN EXPERT) ===
    private String buildSmartGeminiPrompt(CampaignInfo campaignInfo, ProductMetrics metrics, List<Product> products) {
        StringBuilder prompt = new StringBuilder();

        // 1. Role: Senior Business Analyst - Tập trung vào Phân tích Hiệu quả Đầu tư (ROI) và Rủi ro
        prompt.append("Vai trò: Bạn là Chuyên gia Phân tích Dữ liệu Bán lẻ (Senior Retail Analyst) của chuỗi Tech Shop.\n");
        prompt.append("Nhiệm vụ: Đánh giá kịch bản khuyến mãi dựa trên dữ liệu tồn kho và nguyên lý kinh tế học.\n");
        prompt.append("Yêu cầu tông giọng: Nghiêm túc, khách quan, dựa trên số liệu (Data-driven), cảnh báo rủi ro rõ ràng.\n\n");

        // 2. Analytical Framework (Khung phân tích nâng cao)
        prompt.append("=== KHUNG PHÂN TÍCH CHIẾN LƯỢC (LOGIC BẮT BUỘC) ===\n");
        prompt.append("Hãy phân tích theo ma trận Sản phẩm/Biên lợi nhuận:\n");
        prompt.append("1. NHÓM LOW-MARGIN (Điện thoại/Laptop cao cấp, Apple, Flagship):\n");
        prompt.append("   - Đặc điểm: Giá trị cao, lãi gộp mỏng (<10%).\n");
        prompt.append("   - Tác động giảm giá: Giảm >5% sẽ ăn mòn lợi nhuận. Giảm >10% thường gây LỖ VỐN.\n");
        prompt.append("   - Hành vi khách: Nhạy cảm giá thấp. Giảm nhiều chưa chắc tăng vọt doanh số vì cầu đã bão hòa.\n");
        prompt.append("2. NHÓM HIGH-MARGIN (Phụ kiện, Linh kiện, Gaming Gear):\n");
        prompt.append("   - Đặc điểm: Giá trị nhỏ, lãi gộp dày (30-60%).\n");
        prompt.append("   - Tác động giảm giá: Là động lực chính kéo Traffic. Có thể giảm sâu 20-30% vẫn có lãi.\n");
        prompt.append("3. NHÓM DEAD-STOCK (Hàng tồn kho lâu, model cũ):\n");
        prompt.append("   - Chiến lược: Ưu tiên 'Cash Flow' (Dòng tiền) hơn 'Profit'. Cần xả càng nhanh càng tốt.\n\n");

        // 3. Campaign Data
        prompt.append("=== DỮ LIỆU ĐẦU VÀO ===\n");
        prompt.append("- Mô tả chiến dịch: \"").append(campaignInfo.getOriginalDescription()).append("\"\n");
        prompt.append("- Phân khúc mục tiêu: ").append(campaignInfo.getTargetCategory()).append("\n");
        prompt.append("- Mức giảm giá đề xuất: ").append(campaignInfo.getDiscountPercentage()).append("%\n");
        prompt.append("- Tổng tồn kho chịu ảnh hưởng: ").append(metrics.getTotalStock()).append(" sản phẩm.\n");
        prompt.append("- Giá trị trung bình đơn vị: ").append(metrics.getAveragePrice()).append(" VNĐ.\n\n");

        // 4. Product Sample (Cung cấp ngữ cảnh cụ thể để AI không chém gió chung chung)
        prompt.append("=== MẪU SẢN PHẨM CỤ THỂ TRONG KHO (Dùng để phân tích) ===\n");
        List<Product> representativeProducts = getRepresentativeProducts(products);
        for (Product p : representativeProducts) {
            prompt.append("- Product: ").append(p.getName())
                    .append(" | Brand: ").append(p.getBrand() != null ? p.getBrand().getName() : "Unknown")
                    .append(" | Giá bán hiện tại: ").append(p.getPrice())
                    .append(" | Tồn kho: ").append(p.getStockQuantity())
                    .append("\n");
        }

        // 5. Output Requirement
        prompt.append("\n=== YÊU CẦU ĐẦU RA (JSON FORMAT) ===\n");
        prompt.append("Dựa trên logic trên, hãy tính toán và trả về JSON. Tuyệt đối không thêm text bên ngoài JSON.\n");
        prompt.append("{\n");
        prompt.append("  \"salesIncrease\": (int, Tỷ lệ % tăng trưởng số lượng bán so với ngày thường. Hãy thực tế, đừng quá lạc quan),\n");
        prompt.append("  \"predictedSoldUnits\": (int, Số lượng bán dự kiến thực tế dựa trên Tồn kho và Sức hấp dẫn của mức giảm giá),\n");
        prompt.append("  \"revenueGrowth\": (int, % Tăng trưởng doanh thu. Lưu ý: Giảm giá làm giảm giá trị đơn hàng, nếu Sales Volume không tăng đủ bù đắp thì Doanh thu sẽ âm),\n");
        prompt.append("  \"inventoryTurnover\": (int, Điểm tốc độ giải phóng tồn kho 0-100),\n");
        prompt.append("  \"effectivenessScore\": (int, Điểm hiệu quả tổng thể 0-100. Hãy chấm thấp nếu chiến dịch gây lỗ),\n");
        prompt.append("  \"roi\": (int, Tỷ suất hoàn vốn ước tính %. Nếu giảm giá vượt quá biên lợi nhuận ngành (ví dụ giảm iPhone 15%), hãy trả về số ÂM),\n");
        prompt.append("  \"recommendation\": \"(Ngắn gọn: Khuyên dùng / Cần điều chỉnh mức giảm / Rủi ro cao - Không nên)\",\n");
        prompt.append("  \"insights\": \"(Phân tích sắc sảo 3-4 câu. Chỉ ra cụ thể: Với danh sách sản phẩm này, mức giảm này là Hợp lý hay Nguy hiểm? Tại sao? Có sản phẩm nào trong danh sách bị bán lỗ không?)\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    private List<Product> getRepresentativeProducts(List<Product> products) {
        if (products.size() <= 5) return products;

        List<Product> sorted = products.stream()
                .sorted(Comparator.comparing(Product::getPrice))
                .collect(Collectors.toList());

        List<Product> result = new ArrayList<>();
        // Lấy đại diện: Rẻ nhất, Đắt nhất, Giữa
        if (!sorted.isEmpty()) {
            result.add(sorted.get(0));
            result.add(sorted.get(sorted.size() - 1));
            result.add(sorted.get(sorted.size() / 2));
        }

        Random rand = new Random();
        for(int i=0; i<2; i++) {
            if (sorted.size() > 0)
                result.add(sorted.get(rand.nextInt(sorted.size())));
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    private AISimulationResult parseGeminiResponse(Map<String, Object> responseBody, CampaignInfo campaignInfo, ProductMetrics metrics) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) return createDefaultPrediction(campaignInfo, metrics);

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            if (parts == null || parts.isEmpty()) return createDefaultPrediction(campaignInfo, metrics);

            String text = (String) parts.get(0).get("text");
            String jsonText = extractJSON(text);

            AISimulationResult result = new AISimulationResult();
            result.setSalesIncrease(extractNumberFromJSON(jsonText, "salesIncrease"));
            result.setPredictedSoldUnits(extractNumberFromJSON(jsonText, "predictedSoldUnits"));
            result.setRevenueGrowth(extractNumberFromJSON(jsonText, "revenueGrowth"));
            result.setInventoryTurnover(extractNumberFromJSON(jsonText, "inventoryTurnover"));
            result.setEffectivenessScore(extractNumberFromJSON(jsonText, "effectivenessScore"));
            result.setRoi(extractNumberFromJSON(jsonText, "roi"));
            result.setRecommendation(extractStringFromJSON(jsonText, "recommendation"));
            result.setInsights(extractStringFromJSON(jsonText, "insights"));

            return result;
        } catch (Exception e) {
            return createDefaultPrediction(campaignInfo, metrics);
        }
    }

    private String extractJSON(String text) {
        // Xử lý markdown code block nếu AI trả về
        text = text.replaceAll("```json", "").replaceAll("```", "").trim();
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1) return text.substring(start, end + 1);
        return text;
    }

    private int extractNumberFromJSON(String json, String key) {
        try {
            // Regex xử lý số âm và khoảng trắng
            Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) return Integer.parseInt(matcher.group(1));
        } catch (Exception e) {}
        return 0;
    }

    private String extractStringFromJSON(String json, String key) {
        try {
            // Regex xử lý chuỗi và các ký tự đặc biệt cơ bản
            Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) return matcher.group(1);
        } catch (Exception e) {}
        return "Không có dữ liệu chi tiết";
    }

    private AISimulationResult createDefaultPrediction(CampaignInfo campaignInfo, ProductMetrics metrics) {
        AISimulationResult result = new AISimulationResult();
        int discount = campaignInfo.getDiscountPercentage();

        // LOGIC TÍNH TAY MỚI (Hào phóng hơn)

        // 1. Tỷ lệ bán được (Sales Rate) dựa trên mức giảm giá
        // Ví dụ: Giảm 0% -> Bán tự nhiên 5% kho
        //        Giảm 10% -> Bán thêm 15% kho -> Tổng 20% kho
        //        Giảm 50% -> Bán thêm 75% kho -> Tổng 80% kho
        double baseSalesRate = 0.05; // Bán tự nhiên 5%
        double elasticFactor = 1.5;  // Hệ số co giãn (1% giảm giá = 1.5% tăng lượng bán)

        double predictedRate = baseSalesRate + (discount * elasticFactor / 100.0);

        // Giới hạn không quá 90% kho (để thực tế)
        if (predictedRate > 0.9) predictedRate = 0.9;

        int predictedUnits = (int) (metrics.getTotalStock() * predictedRate);

        // Nếu số lượng quá ít (do kho ít), ít nhất cũng bán được vài cái
        if (predictedUnits == 0 && metrics.getTotalStock() > 0) predictedUnits = Math.min(metrics.getTotalStock(), 5);

        result.setPredictedSoldUnits(predictedUnits);

        // % Tăng trưởng so với bán tự nhiên
        int normalSales = (int)(metrics.getTotalStock() * 0.05);
        int increasePercent = normalSales > 0 ? ((predictedUnits - normalSales) * 100 / normalSales) : 0;

        result.setSalesIncrease(increasePercent);
        result.setRevenueGrowth(discount); // Tạm tính
        result.setInventoryTurnover(Math.min(100, (int)(predictedRate * 100) * 2)); // Điểm xả kho cao
        result.setEffectivenessScore(60 + (discount / 2)); // Điểm hiệu quả
        result.setRoi(15 - (discount / 3)); // Giảm giá càng sâu ROI càng thấp

        result.setRecommendation("Nên áp dụng (Dữ liệu ước tính)");
        result.setInsights("Hệ thống AI đang bận. Dựa trên mức giảm " + discount + "%, hệ thống ước tính theo công thức tuyến tính sẽ bán được khoảng " + (int)(predictedRate*100) + "% kho hàng.");

        return result;
    }

    private Map<String, Object> buildSimulationResponse(CampaignInfo campaignInfo, ProductMetrics metrics, AISimulationResult aiResult, List<Product> products) {
        Map<String, Object> response = new HashMap<>();
        response.put("campaignDescription", campaignInfo.getOriginalDescription());
        response.put("targetCategory", campaignInfo.getTargetCategory());
        response.put("discountPercentage", campaignInfo.getDiscountPercentage());
        response.put("affectedProducts", products.size());

        BigDecimal avgMonthlyRevenue = metrics.getTotalInventoryValue().divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
        response.put("currentRevenue", avgMonthlyRevenue.doubleValue());

        // Doanh thu dự kiến
        BigDecimal predictedRevenue = avgMonthlyRevenue.multiply(new BigDecimal(1 + aiResult.getRevenueGrowth() / 100.0));
        response.put("predictedRevenue", predictedRevenue.doubleValue());
        response.put("revenueGrowth", aiResult.getRevenueGrowth());

        response.put("currentStock", metrics.getTotalStock());
        response.put("predictedSales", aiResult.getPredictedSoldUnits());
        response.put("remainingStock", Math.max(0, metrics.getTotalStock() - aiResult.getPredictedSoldUnits()));

        // Các chỉ số AI
        response.put("inventoryTurnover", aiResult.getInventoryTurnover());
        response.put("effectivenessScore", aiResult.getEffectivenessScore());
        response.put("predictedROI", aiResult.getRoi());
        response.put("recommendation", aiResult.getRecommendation());
        response.put("aiInsights", aiResult.getInsights());
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        return response;
    }

    // Inner Classes
    private static class CampaignInfo {
        private String originalDescription;
        private String targetCategory;
        private String targetKeyword;
        private String secondaryKeyword; // Thêm keyword phụ cho song ngữ
        private int discountPercentage;

        public String getOriginalDescription() { return originalDescription; }
        public void setOriginalDescription(String originalDescription) { this.originalDescription = originalDescription; }
        public String getTargetCategory() { return targetCategory; }
        public void setTargetCategory(String targetCategory) { this.targetCategory = targetCategory; }
        public String getTargetKeyword() { return targetKeyword; }
        public void setTargetKeyword(String targetKeyword) { this.targetKeyword = targetKeyword; }
        public String getSecondaryKeyword() { return secondaryKeyword; }
        public void setSecondaryKeyword(String secondaryKeyword) { this.secondaryKeyword = secondaryKeyword; }
        public int getDiscountPercentage() { return discountPercentage; }
        public void setDiscountPercentage(int discountPercentage) { this.discountPercentage = discountPercentage; }
    }

    private static class ProductMetrics {
        private int totalProducts;
        private int totalStock;
        private BigDecimal totalInventoryValue;
        private BigDecimal averagePrice;

        public int getTotalProducts() { return totalProducts; }
        public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }
        public int getTotalStock() { return totalStock; }
        public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
        public BigDecimal getTotalInventoryValue() { return totalInventoryValue; }
        public void setTotalInventoryValue(BigDecimal totalInventoryValue) { this.totalInventoryValue = totalInventoryValue; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    }

    private static class AISimulationResult {
        private int salesIncrease;
        private int predictedSoldUnits;
        private int revenueGrowth;
        private int inventoryTurnover;
        private int effectivenessScore;
        private int roi;
        private String recommendation;
        private String insights;

        public int getSalesIncrease() { return salesIncrease; }
        public void setSalesIncrease(int salesIncrease) { this.salesIncrease = salesIncrease; }
        public int getPredictedSoldUnits() { return predictedSoldUnits; }
        public void setPredictedSoldUnits(int predictedSoldUnits) { this.predictedSoldUnits = predictedSoldUnits; }
        public int getRevenueGrowth() { return revenueGrowth; }
        public void setRevenueGrowth(int revenueGrowth) { this.revenueGrowth = revenueGrowth; }
        public int getInventoryTurnover() { return inventoryTurnover; }
        public void setInventoryTurnover(int inventoryTurnover) { this.inventoryTurnover = inventoryTurnover; }
        public int getEffectivenessScore() { return effectivenessScore; }
        public void setEffectivenessScore(int effectivenessScore) { this.effectivenessScore = effectivenessScore; }
        public int getRoi() { return roi; }
        public void setRoi(int roi) { this.roi = roi; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
        public String getInsights() { return insights; }
        public void setInsights(String insights) { this.insights = insights; }
    }
}