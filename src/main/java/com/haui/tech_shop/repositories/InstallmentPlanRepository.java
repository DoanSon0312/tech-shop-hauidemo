package com.haui.tech_shop.repositories;

import com.haui.tech_shop.entities.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {
    List<InstallmentPlan> findByProductId(Long productId);
    List<InstallmentPlan> findByProductIdAndActiveTrue(Long productId);
}
