package com.haui.tech_shop.dtos.responses;

import com.haui.tech_shop.entities.Product;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductImageRes {
    private String url;
    private Product product;
    private boolean isUrlImage;
}
