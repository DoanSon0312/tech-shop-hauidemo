package com.haui.tech_shop.dtos.requests;

import com.haui.tech_shop.enums.InstallmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InstallmentRequestDTO {
    private Long id;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private BigDecimal loanAmount;
    private BigDecimal monthlyPayment;
    private BigDecimal totalInterest;
    private InstallmentStatus status;
    private String documents;

    private Long orderId;
    private Long userId;
    private Long installmentPlanId;
    private Long financePartnerId;
    private Long productId; // Added for creating order if needed
}
