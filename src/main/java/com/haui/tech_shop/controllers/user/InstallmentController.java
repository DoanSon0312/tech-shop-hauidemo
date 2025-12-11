package com.haui.tech_shop.controllers.user;

import com.haui.tech_shop.dtos.requests.InstallmentRequestDTO;
import com.haui.tech_shop.entities.InstallmentRequest;
import com.haui.tech_shop.services.Impl.InstallmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;


@Controller
@RequestMapping("/user/installment")
public class InstallmentController {

    @Autowired
    private InstallmentService installmentService;

    @GetMapping("/calculate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculateInstallment(@RequestParam Long productId,
                                                                    @RequestParam Long planId,
                                                                    @RequestParam Long financePartnerId) {
        try {
            // Gọi service để xử lý tất cả logic
            Map<String, Object> result = installmentService.calculateInstallmentWithValidation(
                    productId, planId, financePartnerId);

            // Kiểm tra kết quả từ service
            if (result.containsKey("success") && !(Boolean) result.get("success")) {
                // Có lỗi từ service
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

            // Thành công
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("Controller: Unexpected error: " + e.getMessage());
            e.printStackTrace();

            // Fallback error response nếu service không xử lý được
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Lỗi hệ thống",
                            "message", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau."
                    ));
        }
    }

    @PostMapping("/submitRequest")
    @ResponseBody
    public ResponseEntity<?> submitInstallmentRequest(@RequestPart("request") InstallmentRequestDTO requestDTO,
                                                      @RequestPart("documents") MultipartFile[] documents) {
        try {
            String uploadDir = "src/main/resources/uploads_installments";
            InstallmentRequest newRequest = installmentService.createInstallmentRequest(requestDTO, documents, uploadDir);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Yêu cầu trả góp đã được gửi thành công",
                    "requestId", newRequest.getId()
            ));

        } catch (RuntimeException e) {
            // Xử lý lỗi order đã có installment request
            if (e.getMessage().contains("Order đã có yêu cầu trả góp")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", e.getMessage()
                        ));
            }
            if (e.getMessage().contains("Vui lòng đăng nhập")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "message", e.getMessage()
                        ));
            }

            System.err.println("Controller: Error submitting installment request: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Đã xảy ra lỗi khi gửi yêu cầu. Vui lòng thử lại."
                    ));
        } catch (Exception e) {
            System.err.println("Controller: Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau."
                    ));
        }
    }

    // Your existing endpoint which requires a requestId
    @GetMapping("/confirmation")
    public String showConfirmationPage(@RequestParam Long requestId, Model model) {
        try {
            InstallmentRequest request = installmentService.getInstallmentRequestById(requestId);

            // Lấy các giá trị cần thiết từ đối tượng request
            Long requestID = request.getId();
            String productName = request.getOrder().getOrderDetails().get(0).getProduct().getName();
            BigDecimal totalAmount = request.getOrder().getTotalPrice();
            BigDecimal monthlyPayment = request.getMonthlyPayment();
            int installmentMonths = request.getInstallmentPlan().getMonths();
            BigDecimal prepayAmount = totalAmount.subtract(request.getLoanAmount());

            // Thêm các thuộc tính vào model với tên khớp với template
            model.addAttribute("requestId", requestID);
            model.addAttribute("productName", productName);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("prepayAmount", prepayAmount); // Tính toán và thêm giá trị trả trước
            model.addAttribute("monthlyPayment", monthlyPayment);
            model.addAttribute("installmentMonths", installmentMonths);

            // Nếu bạn muốn hiển thị toàn bộ đối tượng request, bạn vẫn có thể thêm nó
            // model.addAttribute("request", request);

            return "user/confirmation-page";
        } catch (Exception e) {
            // Ghi log lỗi để dễ dàng gỡ lỗi
            e.printStackTrace();
            model.addAttribute("error", "Không tìm thấy yêu cầu trả góp hoặc dữ liệu bị thiếu.");
            return "error"; // Chuyển hướng đến trang lỗi
        }
    }


}