package com.haui.tech_shop.repositories;

import com.haui.tech_shop.entities.FinancePartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface FinancePartnerRepository extends JpaRepository<FinancePartner, Long> {
    List<FinancePartner> findByActiveTrue();
}
