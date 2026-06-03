package com.isd.wms.repository;

import com.isd.wms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByNameContainingIgnoreCaseAndCategoryId(String name, Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
