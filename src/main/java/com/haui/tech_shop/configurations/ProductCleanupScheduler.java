package com.haui.tech_shop.configurations;

import com.haui.tech_shop.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCleanupScheduler {

    private final IProductService productService;

    // Chạy mỗi ngày lúc 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoDeleteExpiredProducts() {
        log.info("Starting auto-delete expired products job...");

        try {
            int deletedCount = productService.autoDeleteExpiredProducts();
            log.info("Auto-deleted {} expired products", deletedCount);
        } catch (Exception e) {
            log.error("Error during auto-delete expired products: {}", e.getMessage(), e);
        }
    }

    // (Optional) Chạy mỗi giờ để log số sản phẩm sắp bị xóa
    @Scheduled(cron = "0 0 * * * ?")
    public void logPendingDeletions() {
        try {
            long count = productService.countProductsToAutoDelete(); // Gọi qua service
            if (count > 0) {
                log.info("There are {} products pending auto-deletion", count);
            }
        } catch (Exception e) {
            log.error("Error checking pending deletions: {}", e.getMessage());
        }
    }
}