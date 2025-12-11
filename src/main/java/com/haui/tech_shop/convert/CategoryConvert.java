package com.haui.tech_shop.convert;

import com.haui.tech_shop.dtos.requests.CategoryRequest;
import com.haui.tech_shop.entities.Category;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CategoryConvert {
    @Autowired
    private ModelMapper modelMapper;

    public CategoryRequest toDTO(Category category) {
        return modelMapper.map(category, CategoryRequest.class);
    }
    public Category toEntity(CategoryRequest categoryRequest) {
        return modelMapper.map(categoryRequest, Category.class);
    }
}