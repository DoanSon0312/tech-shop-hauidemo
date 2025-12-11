package com.haui.tech_shop.entities;

import com.haui.tech_shop.enums.InstallmentStatus;
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
@Entity(name = "installment_requests")
public class InstallmentRequest extends TrackingDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_amount", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal loanAmount;

    @Column(name = "monthly_payment", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal monthlyPayment;

    @Column(name = "total_interest", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal totalInterest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "documents")
    private String documents;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "installment_plan_id", nullable = false)
    private InstallmentPlan installmentPlan;
}