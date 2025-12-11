package com.haui.tech_shop.repositories;

import com.haui.tech_shop.entities.InstallmentRequest;
import com.haui.tech_shop.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface InstallmentRequestRepository extends JpaRepository<InstallmentRequest, Long> {

    Optional<InstallmentRequest> findByOrderId(Long orderId);

    List<InstallmentRequest> findByStatus(InstallmentStatus status);

    @Query(value = "SELECT * FROM installment_request WHERE some_field LIKE CONCAT('%', ?1, '%')", nativeQuery = true)
    List<InstallmentRequest> searchInstallmentRequests(String keyword);
}