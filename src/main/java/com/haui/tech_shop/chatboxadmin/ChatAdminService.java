package com.haui.tech_shop.chatboxadmin;

import com.haui.tech_shop.chatbox.ConversationContext;
import com.haui.tech_shop.chatbox.GeminiConfig;
import com.haui.tech_shop.entities.*;
import com.haui.tech_shop.enums.OrderStatus;
import com.haui.tech_shop.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatAdminService {
    private final GeminiConfig config;
    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository; // Thêm repository này

    private final Map<String, ConversationContext> adminContexts = new HashMap<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private static final String ADMIN_SYSTEM_INSTRUCTION =
            "Bạn là Trợ lý Quản trị AI thông minh của hệ thống Tech Shop. 📊\n\n" +
                    "KHẢ NĂNG CỦA BẠN:\n" +
                    "1. Phân tích toàn diện dữ liệu: doanh thu, đơn hàng, sản phẩm, khách hàng, tồn kho\n" +
                    "2. Đưa ra insight và xu hướng kinh doanh\n" +
                    "3. Cảnh báo vấn đề tiềm ẩn (hết hàng, đơn chưa xử lý, v.v.)\n" +
                    "4. So sánh và xếp hạng (top sản phẩm, khách hàng VIP, v.v.)\n\n" +
                    "QUY TẮC TRẢ LỜI:\n" +
                    "- Trả lời CỤ THỂ, CHÍNH XÁC dựa trên dữ liệu được cung cấp\n" +
                    "- Sử dụng thẻ HTML: <b>, <br>, <span style='color:...'>\n" +
                    "- TUYỆT ĐỐI KHÔNG dùng Markdown (**, ##, ###)\n" +
                    "- KHÔNG tự ý xuống dòng bằng phím Enter quá nhiều, chỉ xuống dòng khi thực sự cần thiết bằng thẻ <br>\n" +
                    "- Giữ nội dung súc tích, gọn gàng, tránh khoảng trống thừa\n" +
                    "- Highlight số liệu quan trọng bằng <b> và màu sắc\n" +
                    "- Thêm emoji phù hợp: 📈 📉 ⚠️ ✅ 💰 👥 📦 🏆\n" +
                    "- Nếu thiếu dữ liệu, nói rõ và đề xuất admin cần làm gì\n\n" +
                    "PHONG CÁCH:\n" +
                    "- Chuyên nghiệp nhưng thân thiện\n" +
                    "- Chủ động đưa ra gợi ý và hành động tiếp theo\n" +
                    "- Khi phát hiện vấn đề, đưa ra giải pháp cụ thể";

    public String getAdminChatResponse(String message, String adminId) {
        ConversationContext context = adminContexts.computeIfAbsent(adminId, k -> new ConversationContext());
        context.addMessage("user", message);

        // Thu thập TẤT CẢ dữ liệu hệ thống
        String fullSystemData = collectComprehensiveSystemData(message);

        String prompt = String.format(
                "%s\n\n" +
                        "════════════════════════════════════════\n" +
                        "📊 DỮ LIỆU HỆ THỐNG TECH SHOP\n" +
                        "Thời gian: %s\n" +
                        "════════════════════════════════════════\n\n" +
                        "%s\n\n" +
                        "════════════════════════════════════════\n" +
                        "❓ CÂU HỎI CỦA ADMIN:\n%s\n" +
                        "════════════════════════════════════════",
                ADMIN_SYSTEM_INSTRUCTION,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                fullSystemData,
                message
        );

        String aiResponse = callGeminiAPI(prompt, context);
        context.addMessage("assistant", aiResponse);
        return aiResponse;
    }

    /**
     * Thu thập DỮ LIỆU TOÀN DIỆN từ toàn bộ hệ thống
     */
    private String collectComprehensiveSystemData(String query) {
        StringBuilder data = new StringBuilder();

        // 1. TỔNG QUAN HỆ THỐNG
        data.append("1. 🏢 TỔNG QUAN HỆ THỐNG\n");
        data.append(getSystemOverview()).append("\n");

        // 2. SẢN PHẨM - Luôn thu thập để AI hiểu về catalog
        data.append("2. 📦 THÔNG TIN SẢN PHẨM\n");
        data.append(getProductInsights()).append("\n");

        // 3. ĐƠN HÀNG & DOANH THU
        data.append("3. 💰 ĐƠN HÀNG & DOANH THU\n");
        data.append(getOrderInsights()).append("\n");

        // 4. KHÁCH HÀNG
        data.append("4. 👥 KHÁCH HÀNG\n");
        data.append(getCustomerInsights()).append("\n");

        // 5. CẢNH BÁO & VẤN ĐỀ
        data.append("5. ⚠️ CẢNH BÁO & VẤN ĐỀ\n");
        data.append(getAlerts());

        return data.toString();
    }

    /**
     * 1. Tổng quan hệ thống
     */
    private String getSystemOverview() {
        StringBuilder s = new StringBuilder();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();

        s.append("    Tổng sản phẩm: ").append(totalProducts).append("\n");
        s.append("    Tổng đơn hàng: ").append(totalOrders).append("\n");
        s.append("    Tổng khách hàng: ").append(totalUsers);

        return s.toString();
    }

    /**
     * 2. Phân tích chi tiết sản phẩm
     */
    private String getProductInsights() {
        StringBuilder s = new StringBuilder();
        List<Product> allProducts = productRepository.findByActiveTrue();

        // Tổng quan
        s.append("    Tổng số sản phẩm đang bán: ").append(allProducts.size()).append("\n\n");

        // TOP SẢN PHẨM BÁN CHẠY NHẤT (tính từ OrderDetail)
        Map<Long, Integer> productSalesMap = calculateProductSales();

        if (!productSalesMap.isEmpty()) {
            List<Map.Entry<Long, Integer>> topSellingEntries = productSalesMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            s.append("    🏆 TOP 5 SẢN PHẨM BÁN CHẠY NHẤT:\n");
            int rank = 1;
            for (Map.Entry<Long, Integer> entry : topSellingEntries) {
                Product product = productRepository.findById(entry.getKey()).orElse(null);
                if (product != null) {
                    s.append("       ").append(rank++).append(". ")
                            .append(product.getName())
                            .append(" - Đã bán: ").append(entry.getValue()).append(" sản phẩm")
                            .append(" - Giá: ").append(formatCurrency(product.getPrice()))
                            .append("\n");
                }
            }
            s.append("\n");
        } else {
            s.append("    ℹ️ Chưa có dữ liệu bán hàng\n\n");
        }

        // TOP SẢN PHẨM TỒN KHO
        List<Product> topSelling = allProducts.stream()
                .sorted(Comparator.comparing(Product::getStockQuantity).reversed())
                .limit(5)
                .collect(Collectors.toList());

        s.append("   \n");
        s.append("   🏆 TOP 5 SẢN PHẨM (Theo tồn kho - cần thêm logic bán chạy):\n");
        int rank = 1;
        for (Product p : topSelling) {
            s.append("      ").append(rank++).append(". ")
                    .append(p.getName())
                    .append(" - Giá: ").append(formatCurrency(p.getPrice()))
                    .append(" - Tồn: ").append(p.getStockQuantity())
                    .append("\n");
        }

        // SẢN PHẨM SẮP HẾT HÀNG
        List<Product> lowStock = allProducts.stream()
                .filter(p -> p.getStockQuantity() < 10)
                .sorted(Comparator.comparing(Product::getStockQuantity))
                .limit(10)
                .collect(Collectors.toList());

        if (!lowStock.isEmpty()) {
            s.append("    ⚠️ SẢN PHẨM SẮP HẾT HÀNG (<10):\n");
            for (Product p : lowStock) {
                s.append("       ").append(p.getName())
                        .append(" - Còn: ").append(p.getStockQuantity())
                        .append(" - Giá: ").append(formatCurrency(p.getPrice()))
                        .append("\n");
            }
            s.append("\n");
        } else {
            s.append("    ✅ Tất cả sản phẩm đều đủ hàng (>10)\n\n");
        }

        // PHÂN BỐ GIÁ
        if (!allProducts.isEmpty()) {
            BigDecimal avgPrice = allProducts.stream()
                    .map(Product::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(allProducts.size()), 2, BigDecimal.ROUND_HALF_UP);

            s.append("    📊 Giá trung bình: ").append(formatCurrency(avgPrice));
        }

        return s.toString();
    }

    /**
     * Tính toán số lượng bán của từng sản phẩm từ OrderDetail
     */
    private Map<Long, Integer> calculateProductSales() {
        List<Order> completedOrders = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED ||
                        order.getStatus() == OrderStatus.SHIPPING)
                .collect(Collectors.toList());

        Map<Long, Integer> salesMap = new HashMap<>();

        for (Order order : completedOrders) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Long productId = detail.getProduct().getId();
                int quantity = detail.getQuantity();
                salesMap.put(productId, salesMap.getOrDefault(productId, 0) + quantity);
            }
        }

        return salesMap;
    }

    /**
     * 3. Phân tích đơn hàng & doanh thu
     */
    private String getOrderInsights() {
        StringBuilder s = new StringBuilder();
        List<Order> allOrders = orderRepository.findAll();

        s.append("    Tổng số đơn hàng: ").append(allOrders.size()).append("\n");

        if (allOrders.isEmpty()) {
            s.append("    Chưa có đơn hàng nào");
            return s.toString();
        }

        // TỔNG DOANH THU
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        s.append("    💰 Tổng doanh thu: ").append(formatCurrency(totalRevenue)).append("\n");

        // DOANH THU TRUNG BÌNH
        BigDecimal avgOrderValue = totalRevenue.divide(
                BigDecimal.valueOf(allOrders.size()),
                2,
                BigDecimal.ROUND_HALF_UP
        );
        s.append("    Giá trị đơn hàng TB: ").append(formatCurrency(avgOrderValue)).append("\n\n");

        // PHÂN BỐ TRẠNG THÁI ĐƠN HÀNG
        Map<OrderStatus, Long> statusCount = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        s.append("    📋 Phân bổ trạng thái:\n");
        statusCount.forEach((status, count) -> {
            String statusName = getStatusName(status);
            s.append(" ").append(statusName).append(": ").append(count).append(" đơn\n");
        });

        return s.toString();
    }

    /**
     * 4. Phân tích khách hàng
     */
    private String getCustomerInsights() {
        StringBuilder s = new StringBuilder();
        long totalCustomers = userRepository.count();

        s.append("    Tổng số khách hàng: ").append(totalCustomers).append("\n");
        s.append("    [Cần bổ sung] Top khách hàng mua nhiều nhất\n");
        s.append("    [Cần bổ sung] Khách hàng mới trong tháng");

        return s.toString();
    }

    /**
     * 5. Cảnh báo & vấn đề
     */
    private String getAlerts() {
        StringBuilder s = new StringBuilder();
        int alertCount = 0;

        // Sản phẩm hết hàng
        long outOfStock = productRepository.findByActiveTrue().stream()
                .filter(p -> p.getStockQuantity() == 0)
                .count();

        if (outOfStock > 0) {
            s.append("    ⚠️ Có ").append(outOfStock).append(" sản phẩm HẾT HÀNG\n");
            alertCount++;
        }

        // Đơn hàng chờ xử lý
        long pendingOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

        if (pendingOrders > 0) {
            s.append("    ⚠️ Có ").append(pendingOrders).append(" đơn hàng CHỜ XỬ LÝ\n");
            alertCount++;
        }

        if (alertCount == 0) {
            s.append("    ✅ Không có cảnh báo nào. Hệ thống hoạt động tốt!");
        }

        return s.toString();
    }

    // ========== HELPER METHODS ==========

    private String formatCurrency(BigDecimal amount) {
        return currencyFormat.format(amount);
    }

    private String getStatusName(OrderStatus status) {
        switch(status) {
            case PENDING: return "Chờ xử lý";
            case COMPLETED: return "Đã xác nhận";
            case SHIPPING: return "Đang giao";
            case DELIVERED: return "Hoàn thành";
            case CANCELLED: return "Đã hủy";
            default: return "Không xác định";
        }
    }

    /**
     * Gọi Gemini API
     */
    private String callGeminiAPI(String prompt, ConversationContext context) {
        String url = config.getUrl() + "?key=" + config.getApiKey();

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", prompt))));

        Map<String, Object> body = Map.of("contents", contents);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            return cleanMarkdown((String) parts.get(0).get("text"));

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Xin lỗi Admin, hệ thống AI đang gặp sự cố. Vui lòng thử lại sau. " +
                    "Lỗi: " + e.getMessage();
        }
    }

    /**
     * Loại bỏ Markdown và dòng trống thừa
     */
    private String cleanMarkdown(String text) {
        if (text == null) return "";

        String cleaned = text
                // 1. Loại bỏ các ký hiệu Markdown
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("__([^_]+)__", "$1")
                .replaceAll("_([^_]+)_", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("#{1,6}\\s*", "")

                // 2. Xử lý xuống dòng - CHỈ GIỮ LẠI MỘT DÒNG TRỐNG NẾU CẦN THIẾT
                // Xóa dòng chỉ chứa khoảng trắng
                .replaceAll("(?m)^\\s+$", "")
                // Thay thế 2 hoặc nhiều dòng mới liên tiếp bằng 1 dòng mới duy nhất
                .replaceAll("\n{2,}", "\n")
                // Xóa dòng mới ở đầu văn bản
                .replaceAll("(?m)^\n+", "")
                // Xóa dòng mới ở cuối văn bản
                .replaceAll("\n+$", "")
                .trim();

        return cleaned;
    }

    /**
     * Xóa context cuộc hội thoại
     */
    public void clearContext(String adminId) {
        adminContexts.remove(adminId);
    }
}