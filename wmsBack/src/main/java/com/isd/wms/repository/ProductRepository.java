package com.isd.wms.repository;

import com.isd.wms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select product from Product product
            where (:name is null or lower(product.name) like lower(concat('%', :name, '%')))
              and (:categoryId is null or product.category.id = :categoryId)
            """)
    List<Product> search(@Param("name") String name, @Param("categoryId") Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
}
