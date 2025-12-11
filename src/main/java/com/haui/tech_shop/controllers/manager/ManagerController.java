package com.haui.tech_shop.controllers.manager;

import com.haui.tech_shop.dtos.requests.ProfileDto;
import com.haui.tech_shop.dtos.responses.LoyalCustomerRes;
import com.haui.tech_shop.entities.Product;
import com.haui.tech_shop.entities.User;
import com.haui.tech_shop.services.interfaces.IOrderService;
import com.haui.tech_shop.services.interfaces.IProductService;
import com.haui.tech_shop.services.interfaces.IVoucherService;
import com.haui.tech_shop.services.interfaces.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final UserService userService;
    private final IOrderService orderService;
    private final IVoucherService voucherService;
    private final IProductService productService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        session.setAttribute("user", user);

        BigDecimal totalPurchaseDue = orderService.getTotalPurchaseDueForDeliveredOrders();
        model.addAttribute("totalPurchaseDue", totalPurchaseDue);

        int totalAvailableVouchers = voucherService.getAvailableVoucherCount();
        model.addAttribute("totalAvailableVouchers", totalAvailableVouchers);

        int totalProductsSold = orderService.getTotalProductsSold();
        model.addAttribute("totalProductsSold", totalProductsSold);

        int totalProducts = productService.getTotalStockQuantity();
        model.addAttribute("totalProducts", totalProducts);

        int userCount = userService.getCountUsersByRoleUser();
        model.addAttribute("userCount", userCount);

        int shipperCount = userService.getCountUsersByRoleShipper();
        model.addAttribute("shipperCount", shipperCount);

        int totalOrders = orderService.getTotalOrder();
        model.addAttribute("totalOrders", totalOrders);

        int totalOrdersPending = orderService.getTotalOrderForShipping();
        model.addAttribute("totalOrdersPending", totalOrdersPending);

        List<Product> top4Products = productService.getTop4BestSellingProducts();
        model.addAttribute("top4Products", top4Products);

        List<Product> top4NewProducts = productService.get4NewProducts();
        model.addAttribute("top4NewProducts", top4NewProducts);

        List<LoyalCustomerRes> topLoyalCustomers = userService.getTop4LoyalCustomers();
        model.addAttribute("loyalCustomers", topLoyalCustomers);

        Map<String, Object> salesRevenueData = orderService.getSalesRevenueDataLast7Days();
        Map<String, Object> orderStatusData = orderService.getOrderStatusStatistics();
        Map<String, Object> monthlyRevenueData = orderService.getMonthlyRevenueData();

        // Thay đổi: Sử dụng getTopSellingCategories thay vì getProductCategoryDistribution
        // để hiển thị danh mục bán chạy nhất
        Map<String, Object> categoryDistribution = productService.getTopSellingCategories();

        // Add to model
        model.addAttribute("salesRevenueData", salesRevenueData);
        model.addAttribute("orderStatusData", orderStatusData);
        model.addAttribute("monthlyRevenueData", monthlyRevenueData);
        model.addAttribute("categoryDistribution", categoryDistribution);

        return "manager/index";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        ProfileDto profileDto = ProfileDto.builder()
                .username(username)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .gender(user.getGender())
                .dob(user.getDateOfBirth())
                .status(user.isActive())
                .image(user.getImage())
                .build();
        model.addAttribute("profileDto", profileDto);
        return "manager/profile";
    }   

    @GetMapping("/blank")
    public String blankPage() {
        return "manager/pages-blank";
    }
    @GetMapping("/contact")
    public String contactPage() {
        return "manager/pages-contact";
    }
    @GetMapping("/error")
    public String errorPage() {
        return "manager/pages-error-404";
    }
    @GetMapping("/faq")
    public String faqPage() {
        return "manager/pages-faq";
    }
}
