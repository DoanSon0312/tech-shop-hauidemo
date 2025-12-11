package com.haui.tech_shop.repositories;

import com.haui.tech_shop.entities.Order;
import com.haui.tech_shop.entities.User;
import com.haui.tech_shop.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_Username(String userUsername);

    String user(User user);

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> getAllByShipperIdAndStatusOrderByUpdatedAtAsc(Long shipperId, OrderStatus status);

    List<Order> getAllByShipperIdOrderByUpdatedAtAsc(Long shipperId);

    @Query("SELECT o " +
            "FROM orders o " +
            "WHERE YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month AND o.shipper.id = :shipperId AND o.status = 'DELIVERED'")
    List<Order> ordersByYearAndMonthForShipper(@Param("year") int year,
                                               @Param("month") int month,
                                               @Param("shipperId") Long shipperId);

    @Query("SELECT o " +
            "FROM orders o " +
            "WHERE YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month AND o.shipper.id = :shipperId AND o.status = 'DELIVERED'")
    List<Order> totalPriceByYearAndMonthForShipper(@Param("year") int year,
                                                   @Param("month") int month,
                                                   @Param("shipperId") Long shipperId);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o.user, SUM(o.totalPrice) " +
            "FROM orders o " +
            "WHERE o.status = 'DELIVERED' " +
            "GROUP BY o.user " +
            "ORDER BY SUM(o.totalPrice) DESC")
    List<Object[]> findTop4LoyalCustomers();

    Optional<Order> findTopByOrderByCreatedAtDesc();

    // 1. Sales & Revenue - Lấy TẤT CẢ orders trong 7 ngày gần nhất (không chỉ DELIVERED)
    @Query(value = """
    SELECT DATE(o.created_at) AS order_date,
        COUNT(DISTINCT o.id) AS sales_count,
        COALESCE(SUM(o.total_price), 0) AS revenue
    FROM orders o
    WHERE o.created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
      AND o.created_at < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND o.active = 1
    GROUP BY DATE(o.created_at)
    ORDER BY order_date ASC
    """, nativeQuery = true)
    List<Object[]> getSalesAndRevenueLast7Days();

    // 2. Monthly Revenue - Lấy TẤT CẢ orders trong năm hiện tại
    @Query(value = """
    SELECT MONTH(o.created_at) AS month, 
           COALESCE(SUM(o.total_price), 0) AS revenue
    FROM orders o
    WHERE YEAR(o.created_at) = YEAR(CURDATE())
      AND o.active = 1
    GROUP BY MONTH(o.created_at)
    ORDER BY month ASC
    """, nativeQuery = true)
    List<Object[]> getMonthlyRevenueThisYear();

    // 3. Order Status - GIỮ NGUYÊN vì đã đúng
    @Query(value = """
    SELECT o.status,
        COUNT(*) AS count
    FROM orders o
    WHERE o.active = 1
    GROUP BY o.status
    ORDER BY count DESC
    """, nativeQuery = true)
    List<Object[]> getOrderStatusCounts();

    // 4. Category Distribution
    @Query(value = """
    SELECT COALESCE(c.name, 'Uncategorized') AS category_name,
           SUM(oi.quantity) AS total_sold
    FROM order_items oi
    INNER JOIN products p ON oi.product_id = p.id
    LEFT JOIN categories c ON p.category_id = c.id
    INNER JOIN orders o ON oi.order_id = o.id
    WHERE o.active = 1
    GROUP BY c.id, c.name
    ORDER BY total_sold DESC
    LIMIT 7
    """, nativeQuery = true)
    List<Object[]> getCategoryDistribution();


}
