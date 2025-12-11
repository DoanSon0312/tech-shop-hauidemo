package com.haui.tech_shop.dtos.requests;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BrandRequest {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name="brand_img", length = 500)
    private String brandImg;

    private boolean active;
}
