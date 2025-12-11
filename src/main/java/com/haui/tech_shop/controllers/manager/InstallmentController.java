package com.haui.tech_shop.controllers.manager;

import com.haui.tech_shop.entities.InstallmentRequest;
import com.haui.tech_shop.enums.InstallmentStatus;
import com.haui.tech_shop.services.Impl.InstallmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller("managerInstallmentController")
@RequestMapping("/manager/installments")
@RequiredArgsConstructor
public class InstallmentController {
    private final InstallmentService installmentService;

    @GetMapping("") // localhost:8080/manager/installments
    public String index(Model model,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) InstallmentStatus status) {

        List<InstallmentRequest> installments;

        if (keyword != null && !keyword.trim().isEmpty()) {
            installments = installmentService.searchInstallmentRequests(keyword);
        } else if (status != null) {
            installments = installmentService.getInstallmentRequestsByStatus(status);
        } else {
            installments = installmentService.getAllInstallmentRequests();
        }

        model.addAttribute("installments", installments);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("statusOptions", InstallmentStatus.values());

        return "manager/installments/installmentList";
    }

    @GetMapping("/detail")
    public String viewInstallmentDetail(Model model, @RequestParam Long id) {
        InstallmentRequest installment = installmentService.getInstallmentRequestById(id);
        if (installment != null) {
            model.addAttribute("installment", installment);
            model.addAttribute("statusOptions", InstallmentStatus.values());
            return "manager/installments/installmentDetail";
        } else {
            model.addAttribute("error", "Không tìm thấy yêu cầu trả góp với ID: " + id);
            return "error";
        }
    }

    @GetMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable(value = "id") Long id) {
        Map<String, String> response = new HashMap<>();
        if (installmentService.deleteById(id)) {
            response.put("status", "success");
            response.put("message", "Installment request deleted successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to delete installment request.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/pending")
    public String pending(@RequestParam Long id,
                          @RequestParam(defaultValue = "") String keyword,
                          @RequestParam(required = false) InstallmentStatus status) {
        installmentService.updateInstallmentStatus(id, InstallmentStatus.PENDING);
        String redirectUrl = "redirect:/manager/installments";
        if (keyword != null && !keyword.isEmpty()) {
            redirectUrl += "?keyword=" + keyword;
        }
        if (status != null) {
            redirectUrl += (keyword != null ? "&" : "?") + "status=" + status;
        }
        return redirectUrl;
    }

    @PostMapping("/approved")
    public String approved(@RequestParam Long id,
                           @RequestParam(defaultValue = "") String keyword,
                           @RequestParam(required = false) InstallmentStatus status) {
        installmentService.updateInstallmentStatus(id, InstallmentStatus.APPROVED);
        String redirectUrl = "redirect:/manager/installments";
        if (keyword != null && !keyword.isEmpty()) {
            redirectUrl += "?keyword=" + keyword;
        }
        if (status != null) {
            redirectUrl += (keyword != null ? "&" : "?") + "status=" + status;
        }
        return redirectUrl;
    }

    @PostMapping("/rejected")
    public String rejected(@RequestParam Long id,
                           @RequestParam(defaultValue = "") String keyword,
                           @RequestParam(required = false) InstallmentStatus status) {
        installmentService.updateInstallmentStatus(id, InstallmentStatus.REJECTED);
        String redirectUrl = "redirect:/manager/installments";
        if (keyword != null && !keyword.isEmpty()) {
            redirectUrl += "?keyword=" + keyword;
        }
        if (status != null) {
            redirectUrl += (keyword != null ? "&" : "?") + "status=" + status;
        }
        return redirectUrl;
    }

    @PostMapping("/completed")
    public String completed(@RequestParam Long id,
                            @RequestParam(defaultValue = "") String keyword,
                            @RequestParam(required = false) InstallmentStatus status) {
        installmentService.updateInstallmentStatus(id, InstallmentStatus.COMPLETED);
        String redirectUrl = "redirect:/manager/installments";
        if (keyword != null && !keyword.isEmpty()) {
            redirectUrl += "?keyword=" + keyword;
        }
        if (status != null) {
            redirectUrl += (keyword != null ? "&" : "?") + "status=" + status;
        }
        return redirectUrl;
    }
}