package com.haui.tech_shop.vnpay;

import com.haui.tech_shop.entities.Order;
import com.haui.tech_shop.services.Impl.OrderServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
@RequestMapping()
public class VNPayController {
    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private OrderServiceImpl orderService;

    // Chuyển hướng người dùng đến cổng thanh toán VNPAY
    @GetMapping({"/user/checkout/vnpay"})
    public String submitOrder(HttpServletRequest request, HttpSession session) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        BigDecimal totalPriceToPayment = (BigDecimal) session.getAttribute("totalPriceToPayment");

        // Lấy orderId từ session (đã được tạo trong quá trình checkout)
        Long orderId = (Long) session.getAttribute("currentOrderId");

        // Tạo orderInfo chứa orderId để sử dụng khi return từ VNPay
        String orderInfo = "Thanh toán đơn hàng #" + orderId;

        String vnpayUrl = vnPayService.createOrder(request, totalPriceToPayment.intValue(), baseUrl, orderInfo);
        return "redirect:" + vnpayUrl;
    }

    // Sau khi hoàn tất thanh toán, VNPAY sẽ chuyển hướng trình duyệt về URL này
    @GetMapping("/vnpay-payment-return")
    public String paymentCompleted(HttpServletRequest request, Model model, HttpSession session) {
        int paymentStatus = vnPayService.orderReturn(request);

        // Lấy thông tin từ VNPAY trả về
        String orderInfo = request.getParameter("vnp_OrderInfo"); // VD: "Thanh toán đơn hàng #51"
        String paymentTime = request.getParameter("vnp_PayDate");
        String transactionId = request.getParameter("vnp_TransactionNo");
        String totalPrice = request.getParameter("vnp_Amount");

        // Định dạng thời gian
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(paymentTime, inputFormatter);
        String formattedDate = dateTime.format(outputFormatter);

        // Chuyển số tiền về dạng VND
        String totalPriceVND = totalPrice.substring(0, totalPrice.length() - 2);

        // Lấy orderId từ chuỗi orderInfo (tách số sau dấu #)
        Long orderId = null;
        if (orderInfo != null && orderInfo.contains("#")) {
            try {
                orderId = Long.parseLong(orderInfo.substring(orderInfo.indexOf("#") + 1).trim());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        model.addAttribute("orderId", orderId);
        model.addAttribute("totalPrice", totalPriceVND);
        model.addAttribute("paymentTime", formattedDate);
        model.addAttribute("transactionId", transactionId);

        if (paymentStatus == 1 && orderId != null) {
            // ✅ Thanh toán thành công → cập nhật trạng thái đơn hàng
            orderService.orderPending(orderId); // hoặc orderShipping() theo logic của bạn

            Optional<Order> orderOptional = orderService.findById(orderId);
            orderOptional.ifPresent(order -> model.addAttribute("order", order));

            return "user/orderSuccess"; // trang thông báo thành công
        } else {
            // ❌ Thanh toán thất bại
            orderService.deleteFailOrder();
            return "user/orderFail";
        }
    }
}

