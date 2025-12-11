package com.haui.tech_shop.repositories;

import com.haui.tech_shop.entities.Brand;
import com.haui.tech_shop.entities.Category;
import com.haui.tech_shop.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByNameContaining(String name);

    Product findByName(String name);

    @Query("SELECT p FROM Product p WHERE p.cpu LIKE %?1%")
    List<Product> findByCpu(String cpu);

    @Query("SELECT p FROM Product p WHERE p.ram LIKE %?1%")
    List<Product> findByRam(String ram);

    @Query("SELECT p FROM Product p WHERE p.os LIKE %?1%")
    List<Product> findByOs(String os);

    @Query("SELECT p FROM Product p WHERE p.monitor LIKE %?1%")
    List<Product> findByMonitor(String monitor);

    @Query("SELECT p FROM Product p WHERE p.weight = ?1")
    List<Product> findByWeight(Double weight);

    @Query("SELECT p FROM Product p WHERE p.battery LIKE %?1%")
    List<Product> findByBattery(String battery);

    @Query("SELECT p FROM Product p WHERE p.graphicCard LIKE %?1%")
    List<Product> findByGraphicCard(String graphicCard);

    @Query("SELECT p FROM Product p WHERE p.port LIKE %?1%")
    List<Product> findByPort(String port);

    @Query("SELECT p FROM Product p WHERE p.rearCamera LIKE %?1%")
    List<Product> findByRearCamera(String rearCamera);

    @Query("SELECT p FROM Product p WHERE p.frontCamera LIKE %?1%")
    List<Product> findByFrontCamera(String frontCamera);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity = ?1")
    List<Product> findByStockQuantity(int stockQuantity);

    List<Product> findByCategoryName(String categoryName);

    List<Product> findByBrandName(String brandName);

    // Paging and Sorting methods
    Page<Product> findAll(Pageable pageable);

    List<Product> findTop4ByOrderByCreatedAtDesc();

    List<Product> findByBrandNameAndAndCategoryName(String brandName, String categoryName);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByDescriptionContainingIgnoreCase(String description);

    @Query(value = """
        SELECT c.name AS category_name, COALESCE(SUM(od.quantity), 0) AS total_sold
        FROM order_details od
        JOIN products p ON od.product_id = p.id
        JOIN categories c ON p.category_id = c.id
        JOIN orders o ON od.order_id = o.id
        WHERE o.status = 'DELIVERED'
        GROUP BY c.id, c.name
        ORDER BY total_sold DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> getTopSellingCategoriesRaw();

    // Product Category Distribution - Tổng số sản phẩm theo category
    @Query(value = """
        SELECT c.name AS category_name, COUNT(p.id) AS product_count
        FROM products p
        JOIN categories c ON p.category_id = c.id
        GROUP BY c.id, c.name
        ORDER BY product_count DESC
        """, nativeQuery = true)
    List<Object[]> getCategoryDistribution();


    List<Product> findByActiveTrue();
    List<Product> findByActiveFalse();
    // Tìm sản phẩm active theo category
    List<Product> findByActiveTrueAndCategory(Category category);

    // Tìm sản phẩm active theo brand
    List<Product> findByActiveTrueAndBrand(Brand brand);

    // Tìm sản phẩm active theo id
    Optional<Product> findByIdAndActiveTrue(Long id);

    // Search sản phẩm active
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchActiveProducts(@Param("keyword") String keyword);

    Page<Product> findByActiveTrue(Pageable pageable);

    // THÊM METHOD NÀY - Tìm sản phẩm đã xóa quá 30 ngày
    @Query("SELECT p FROM Product p WHERE p.active = false AND p.deletedAt < :date")
    List<Product> findByActiveFalseAndDeletedAtBefore(@Param("date") LocalDateTime date);

    // Đếm số sản phẩm sắp bị xóa tự động
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = false AND p.deletedAt < :date")
    long countProductsToAutoDelete(@Param("date") LocalDateTime date);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.name) LIKE %:keyword% " +
            "OR LOWER(p.description) LIKE %:keyword% " +
            "OR LOWER(p.category.name) LIKE %:keyword%)")
    List<Product> findByFlexibleKeyword(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.name) LIKE %:key1% OR LOWER(p.description) LIKE %:key1% " +
            "OR LOWER(p.name) LIKE %:key2% OR LOWER(p.description) LIKE %:key2%)")
    List<Product> findByDualKeyword(@Param("key1") String key1, @Param("key2") String key2);
}
