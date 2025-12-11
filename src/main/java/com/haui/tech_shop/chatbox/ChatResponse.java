package com.haui.tech_shop.chatbox;

import com.haui.tech_shop.entities.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    private String message;
    private List<ProductDTO> products;

    // Static factory method thay vì constructor
    public static ChatResponse fromProducts(String message, List<Product> productEntities) {
        ChatResponse response = new ChatResponse();
        response.setMessage(message);

        if (productEntities != null && !productEntities.isEmpty()) {
            response.setProducts(
                    productEntities.stream()
                            .map(ProductDTO::fromEntity)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private Long id;
        private String name;
        private String description;
        private String price;
        private String cpu;
        private String ram;
        private String battery;
        private String graphicCard;
        private String monitor;
        private String os;
        private String thumbnail;
        private String warranty;
        private String brandName;
        private String categoryName;

        public static ProductDTO fromEntity(Product product) {
            ProductDTO dto = new ProductDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setDescription(product.getDescription());
            dto.setPrice(product.getPrice() != null ? product.getPrice().toString() : "0");
            dto.setCpu(product.getCpu());
            dto.setRam(product.getRam());
            dto.setBattery(product.getBattery());
            dto.setGraphicCard(product.getGraphicCard());
            dto.setMonitor(product.getMonitor());
            dto.setOs(product.getOs());
            dto.setThumbnail(product.getThumbnail());
            dto.setWarranty(product.getWarranty());
            dto.setBrandName(product.getBrand() != null ? product.getBrand().getName() : "");
            dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : "");
            return dto;
        }
    }
}