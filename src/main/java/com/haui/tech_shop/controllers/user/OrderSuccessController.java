package com.haui.tech_shop.controllers.user;

import com.haui.tech_shop.entities.Order;
import com.haui.tech_shop.services.Impl.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class OrderSuccessController {

    @Autowired
    private OrderServiceImpl orderService;

    @GetMapping("/order-success")
    public String orderSuccess(@RequestParam("orderId") Long orderId,
                               @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                               Model model, HttpSession session) {
        try {
            // Lấy thông tin order từ service
            Optional<Order> orderOptional = orderService.findById(orderId);

            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();

                // Thêm thông tin order vào model
                model.addAttribute("order", order);
                model.addAttribute("orderId", order.getId());

                // Format tổng tiền theo định dạng VND
                String totalPriceFormatted = formatVND(order.getTotalPrice());
                model.addAttribute("totalPrice", totalPriceFormatted);

                // Xử lý paymentMethod: Ưu tiên từ param, nếu không có thì lấy từ order.getPayment().getName() (giả sử Order có getPayment())
                String method;
                if (paymentMethod != null) {
                    method = paymentMethod.toLowerCase();
                } else if (order.getPayment() != null) {
                    method = order.getPayment().getName().toLowerCase();
                } else {
                    method = "cod";  // Default
                }
                model.addAttribute("paymentMethod", method);

                // Lấy thời gian thanh toán/đặt hàng: Sử dụng thời gian hiện tại (vì không có getPaymentDate)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                String paymentTime = LocalDateTime.now().format(formatter);
                model.addAttribute("paymentTime", paymentTime);

                // Xử lý transactionId dựa trên paymentMethod (vì không có getTransactionId)
                String transactionId;
                if ("cod".equals(method)) {
                    transactionId = "COD - Không áp dụng";
                } else {
                    transactionId = "Không áp dụng";  // Hoặc lấy từ session/param nếu có, nhưng tạm thời như vậy
                }
                model.addAttribute("transactionId", transactionId);

                return "user/orderSuccess"; // Đảm bảo trả về đúng tên template
            } else {
                model.addAttribute("error", "Không tìm thấy đơn hàng với ID: " + orderId);
                return "redirect:/user/order-fail?error=Order not found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Đã xảy ra lỗi khi xử lý đơn hàng");
            return "redirect:/user/order-fail?error=Processing error";
        }
    }

    // Phương thức định dạng tiền VND
    private String formatVND(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        return formatter.format(amount) + " đ";
    }
}