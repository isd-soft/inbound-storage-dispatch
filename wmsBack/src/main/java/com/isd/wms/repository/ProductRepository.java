package com.isd.wms.repository;

import com.isd.wms.dto.product.ProductWithQuantityProjection;
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

    @Query("""
            SELECT p.id AS id, p.name AS name, SUM(s.quantity - s.reservedQuantity) AS quantity FROM Product p
            JOIN Stock s ON p = s.product
            GROUP BY p.id, p.name
            HAVING SUM(s.quantity - s.reservedQuantity) > 0
            """)
    List<ProductWithQuantityProjection> getProductsWithQuantities();
    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
}
