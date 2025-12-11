package com.haui.tech_shop.repositories;

import com.haui.tech_shop.entities.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByProductId(long productId);
    List<OrderDetail> findByOrderId(long orderId);
    @Query("SELECT od.product, SUM(od.quantity) AS totalSold " +
            "FROM order_details od " +
            "JOIN od.order o " +
            "WHERE o.status = 'DELIVERED' " +
            "GROUP BY od.product " +
            "ORDER BY totalSold DESC")
    List<Object[]> findTop4BestSellingProducts();
    @Query("SELECT COUNT(od) FROM order_details od WHERE od.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
}
