package com.haui.tech_shop.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "installment_plans")
public class InstallmentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "months", nullable = false)
    private int months;

    @Column(name = "prepay_percent", nullable = false, columnDefinition = "DECIMAL(5,2)")
    private BigDecimal prepayPercent;

    @Column(name = "interest_rate", nullable = false, columnDefinition = "DECIMAL(5,2)")
    private BigDecimal interestRate;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "finance_partner_id", nullable = false)
    private FinancePartner financePartner;
}
