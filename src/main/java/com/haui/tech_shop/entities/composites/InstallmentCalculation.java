package com.haui.tech_shop.entities.composites;

import com.haui.tech_shop.entities.InstallmentPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InstallmentCalculation{
    private InstallmentPlan installmentPlan;
    private BigDecimal prepayAmount;
    private BigDecimal loanAmount;
    private BigDecimal monthlyPayment;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
}
