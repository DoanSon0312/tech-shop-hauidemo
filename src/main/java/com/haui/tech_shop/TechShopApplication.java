package com.haui.tech_shop;

import com.haui.tech_shop.services.interfaces.IProductService;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class TechShopApplication implements CommandLineRunner {
	@Resource
	IProductService productService;

	public static void main(String[] args) {
		SpringApplication.run(TechShopApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		productService.init();
	}
}
