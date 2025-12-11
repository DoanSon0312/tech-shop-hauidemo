package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.dtos.requests.CategoryRequest;
import com.haui.tech_shop.entities.Category;

import java.util.List;
import java.util.Optional;

public interface ICategoryService {
    List<Category> findAll();
    Optional<Category> findById(Long id);
    Category findByCategoryName(String categoryName);
    boolean addCategory(CategoryRequest categoryRequest);
    boolean updateCategory(CategoryRequest categoryRequest, Long id);
    boolean deleteCategory(Long id);
}
