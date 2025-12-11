package com.haui.tech_shop.repositories;

import com.haui.tech_shop.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findCategoriesByName(String name);

    Optional<Category> findCategoryByName (String categoryName);

}
