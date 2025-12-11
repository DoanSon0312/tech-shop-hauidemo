package com.haui.tech_shop.convert;

import com.haui.tech_shop.builder.ProductFilterBuilder;
import com.haui.tech_shop.utils.MapUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductFilterBuilderConverter {

    public ProductFilterBuilder toProductFilterBuilder(Map<String, Object> params) {
        ProductFilterBuilder.Builder builder = new ProductFilterBuilder.Builder()
                .setName(MapUtil.getObject(params, "name", String.class))
                .setCategoryName(MapUtil.getObject(params, "categoryName", String.class))
                .setBrandNames(MapUtil.getObject(params, "brandNames", List.class))
                .setRams(MapUtil.getObject(params, "rams", List.class))
                .setMinPrice(MapUtil.getObject(params, "minPrice", Long.class))
                .setMaxPrice(MapUtil.getObject(params, "maxPrice", Long.class));

        // SỬA PHẦN NÀY - XỬ LÝ ACTIVE AN TOÀN
        if (params.containsKey("active")) {
            Object activeValue = params.get("active");
            if (activeValue != null) {
                if (activeValue instanceof Boolean) {
                    builder.setActive((Boolean) activeValue);
                } else if (activeValue instanceof String) {
                    builder.setActive(Boolean.parseBoolean((String) activeValue));
                }
            }
        }

        return builder.build();
    }
}