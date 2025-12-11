package com.haui.tech_shop.services.Impl;

import com.haui.tech_shop.dtos.requests.InstallmentRequestDTO;
import com.haui.tech_shop.entities.*;
import com.haui.tech_shop.repositories.*;
import com.haui.tech_shop.entities.composites.InstallmentCalculation;
import com.haui.tech_shop.enums.InstallmentStatus;
import com.haui.tech_shop.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InstallmentService {

    @Autowired
    private FinancePartnerRepository financePartnerRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private InstallmentRequestRepository installmentRequestRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public List<FinancePartner> getAllActiveFinancePartners() {
        return financePartnerRepository.findByActiveTrue();
    }

    public List<InstallmentPlan> getInstallmentPlansByProduct(Long productId) {
        return installmentPlanRepository.findByProductIdAndActiveTrue(productId);
    }
    public InstallmentRequest getInstallmentRequestById(Long requestId) {
        return installmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu trả góp với ID: " + requestId));
    }
    /**
     * Tính toán trả góp với xử lý lỗi và validation đầy đủ
     */
    public Map<String, Object> calculateInstallmentWithValidation(Long productId, Long planId, Long financePartnerId) {
        try {
            // Validate input parameters
            if (productId == null || planId == null || financePartnerId == null) {
                throw new IllegalArgumentException("ProductId, PlanId và FinancePartnerId không được để trống");
            }

            // Lấy sản phẩm từ DB
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

            BigDecimal productPrice = product.getPrice();
            if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Giá sản phẩm không hợp lệ");
            }

            // Lấy gói trả góp
            InstallmentPlan plan = installmentPlanRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy gói trả góp với ID: " + planId));

            // Kiểm tra plan có active không
            if (!plan.isActive()) {
                throw new RuntimeException("Gói trả góp này hiện không khả dụng");
            }

            // Lấy đối tác tài chính
            FinancePartner financePartner = financePartnerRepository.findById(financePartnerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đối tác tài chính với ID: " + financePartnerId));

            // Gọi hàm tính toán mới với đối tác tài chính
            InstallmentCalculation calculation = calculateInstallment(productPrice, plan, financePartner);

            // Tạo response Map
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prepayAmount", calculation.getPrepayAmount());
            response.put("loanAmount", calculation.getLoanAmount());
            response.put("monthlyPayment", calculation.getMonthlyPayment());
            response.put("totalAmount", calculation.getTotalAmount());
            response.put("totalInterest", calculation.getTotalInterest());

            // Thông tin plan
            Map<String, Object> planInfo = new HashMap<>();
            planInfo.put("id", plan.getId());
            planInfo.put("months", plan.getMonths());
            planInfo.put("interestRate", plan.getInterestRate());
            planInfo.put("prepayPercent", plan.getPrepayPercent());
            response.put("installmentPlan", planInfo);

            // Thông tin đối tác tài chính
            Map<String, Object> partnerInfo = new HashMap<>();
            partnerInfo.put("id", financePartner.getId());
            partnerInfo.put("name", financePartner.getName());
            response.put("financePartner", partnerInfo);

            // Thông tin sản phẩm
            Map<String, Object> productInfo = new HashMap<>();
            productInfo.put("id", product.getId());
            productInfo.put("name", product.getName());
            response.put("product", productInfo);

            return response;

        } catch (IllegalArgumentException e) {
            return createErrorResponse("Tham số không hợp lệ", e.getMessage());
        } catch (RuntimeException e) {
            return createErrorResponse("Lỗi nghiệp vụ", e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Lỗi hệ thống", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        }
    }

    /**
     * Tạo response lỗi thống nhất
     */
    private Map<String, Object> createErrorResponse(String errorType, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorType);
        errorResponse.put("message", message);
        return errorResponse;
    }

    /**
     * Hàm tính toán trả góp mới với đối tác tài chính
     */
    public InstallmentCalculation calculateInstallment(BigDecimal productPrice, InstallmentPlan plan, FinancePartner financePartner) {
        try {
            InstallmentCalculation calculation = new InstallmentCalculation();

            // Trả trước (sử dụng % từ plan)
            BigDecimal prepayAmount = productPrice.multiply(plan.getPrepayPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Số tiền vay
            BigDecimal loanAmount = productPrice.subtract(prepayAmount);

            // Lãi suất tháng (có thể điều chỉnh theo đối tác tài chính)
            BigDecimal effectiveInterestRate = calculateEffectiveInterestRate(plan.getInterestRate(), financePartner);
            BigDecimal monthlyInterestRate = effectiveInterestRate
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

            BigDecimal monthlyPayment;
            if (monthlyInterestRate.compareTo(BigDecimal.ZERO) == 0) {
                // Nếu lãi suất = 0, chia đều số tiền
                monthlyPayment = loanAmount.divide(BigDecimal.valueOf(plan.getMonths()), 2, RoundingMode.HALF_UP);
            } else {
                // Công thức tính trả góp: PMT = [P * r * (1 + r)^n] / [(1 + r)^n - 1]
                double temp = 1 + monthlyInterestRate.doubleValue();
                double powResult = Math.pow(temp, plan.getMonths());

                BigDecimal numerator = loanAmount.multiply(monthlyInterestRate)
                        .multiply(BigDecimal.valueOf(powResult));
                BigDecimal denominator = BigDecimal.valueOf(powResult).subtract(BigDecimal.ONE);

                monthlyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
            }

            // Tổng tiền trả
            BigDecimal totalAmount = prepayAmount.add(monthlyPayment.multiply(BigDecimal.valueOf(plan.getMonths())));

            // Tổng lãi
            BigDecimal totalInterest = totalAmount.subtract(productPrice);

            calculation.setPrepayAmount(prepayAmount);
            calculation.setLoanAmount(loanAmount);
            calculation.setMonthlyPayment(monthlyPayment);
            calculation.setTotalAmount(totalAmount);
            calculation.setTotalInterest(totalInterest);
            calculation.setInstallmentPlan(plan);

            return calculation;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính toán trả góp: " + e.getMessage(), e);
        }
    }

    /**
     * Tính lãi suất hiệu dụng dựa trên đối tác tài chính
     * Có thể áp dụng các chính sách khuyến mãi, phí, etc.
     */
    private BigDecimal calculateEffectiveInterestRate(BigDecimal baseInterestRate, FinancePartner financePartner) {
        // Lấy lãi suất mặc định từ đối tác tài chính.
        // Dữ liệu này có thể được sử dụng làm cơ sở để tính toán.
        return financePartner.getDefaultInterestRate();
    }

    public InstallmentRequest createInstallmentRequest(InstallmentRequestDTO requestDTO,
                                                       MultipartFile[] documents,
                                                       String uploadDir) throws IOException {

        // Get current authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (requestDTO.getUserId() == null) {
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("Vui lòng đăng nhập để gửi yêu cầu trả góp");
            }
            String username = auth.getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            requestDTO.setUserId(currentUser.getId());
        }

        if (requestDTO.getOrderId() == null) {
            if (requestDTO.getProductId() == null) {
                throw new RuntimeException("Yêu cầu productId hoặc orderId");
            }
            User user = userRepository.findById(requestDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + requestDTO.getUserId()));
            Product product = productRepository.findById(requestDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + requestDTO.getProductId()));

            // TẠO PAYMENT MẶC ĐỊNH CHO TRẢ GÓP
            Payment installmentPayment = paymentRepository.findByName("Installment");
            Order newOrder = new Order();
            newOrder.setUser(user);
            newOrder.setTotalPrice(product.getPrice());
            newOrder.setStatus(OrderStatus.PENDING);
            newOrder.setPayment(installmentPayment); // SỬ DỤNG PAYMENT ĐÃ CÓ
            newOrder.setCreatedAt(LocalDate.now());
            newOrder.setUpdatedAt(LocalDate.now());
            newOrder.setActive(true);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(newOrder);
            detail.setProduct(product);
            detail.setQuantity(1);
            detail.setPrice(product.getPrice());
            // TÍNH TOÁN VÀ GÁN GIÁ TRỊ total_price CHO CHI TIẾT ĐƠN HÀNG
            BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            detail.setTotalPrice(totalPrice); // <-- THÊM DÒNG NÀY VÀO
            newOrder.setOrderDetails(List.of(detail));

            orderRepository.save(newOrder);

            requestDTO.setOrderId(newOrder.getId());
        }

        // KIỂM TRA ORDER ĐÃ CÓ INSTALLMENT REQUEST CHƯA
        Optional<InstallmentRequest> existingRequest = installmentRequestRepository.findByOrderId(requestDTO.getOrderId());
        if (existingRequest.isPresent()) {
            throw new RuntimeException("Order đã có yêu cầu trả góp. Mã yêu cầu: " + existingRequest.get().getId());
        }

        // Lưu tài liệu upload
        StringBuilder documentPaths = new StringBuilder();
        for (MultipartFile document : documents) {
            if (!document.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + document.getOriginalFilename();
                Path filePath = Paths.get(uploadDir, fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, document.getBytes());

                if (documentPaths.length() > 0) {
                    documentPaths.append(",");
                }
                documentPaths.append(fileName);
            }
        }

        // Lấy entity liên quan từ DTO
        Order order = orderRepository.findById(requestDTO.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + requestDTO.getOrderId()));
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestDTO.getUserId()));
        InstallmentPlan plan = installmentPlanRepository.findById(requestDTO.getInstallmentPlanId())
                .orElseThrow(() -> new RuntimeException("InstallmentPlan not found with id: " + requestDTO.getInstallmentPlanId()));

        // Lấy thông tin sản phẩm và đối tác tài chính
        Product product = order.getOrderDetails().get(0).getProduct();
        FinancePartner financePartner = financePartnerRepository.findById(requestDTO.getFinancePartnerId())
                .orElseThrow(() -> new RuntimeException("FinancePartner not found with id: " + requestDTO.getFinancePartnerId()));

        // TÍNH TOÁN LẠI monthlyPayment và totalInterest TẠI BACKEND
        InstallmentCalculation calculation = calculateInstallment(product.getPrice(), plan, financePartner);

        // Tạo entity InstallmentRequest
        InstallmentRequest request = new InstallmentRequest();
        request.setCreatedAt(LocalDate.now());
        request.setUpdatedAt(LocalDate.now());
        request.setLoanAmount(calculation.getLoanAmount()); // Lấy từ kết quả tính toán
        request.setMonthlyPayment(calculation.getMonthlyPayment()); // Lấy từ kết quả tính toán
        request.setTotalInterest(calculation.getTotalInterest()); // Lấy từ kết quả tính toán
        request.setStatus(InstallmentStatus.PENDING);
        request.setDocuments(documentPaths.toString());
        request.setOrder(order);
        request.setUser(user);
        request.setInstallmentPlan(plan);

        return installmentRequestRepository.save(request);
    }

    public List<InstallmentRequest> getAllInstallmentRequests() {
        return installmentRequestRepository.findAll();
    }

    public List<InstallmentRequest> getInstallmentRequestsByStatus(InstallmentStatus status) {
        return installmentRequestRepository.findByStatus(status);
    }

    public InstallmentRequest updateInstallmentStatus(Long requestId, InstallmentStatus newStatus) {
        InstallmentRequest request = installmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu trả góp với ID: " + requestId));

        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDate.now());

        return installmentRequestRepository.save(request);
    }


    public List<InstallmentRequest> searchInstallmentRequests(String keyword) {
        return installmentRequestRepository.searchInstallmentRequests(keyword);
    }
    public boolean deleteById(Long id) {
        if (installmentRequestRepository.existsById(id)) {
            installmentRequestRepository.deleteById(id);
            return true;
        }
        return false;
    }
}