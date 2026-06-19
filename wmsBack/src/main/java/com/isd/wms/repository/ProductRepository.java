package com.isd.wms.repository;

import com.isd.wms.entity.Product;
import com.isd.wms.enums.Zone;
import com.isd.wms.repository.projections.ProductWithQuantityProjection;
import com.isd.wms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        select product from Product product
        where (:name is null or lower(product.name) like lower(concat('%', :name, '%')))
          and (:categoryId is null or product.category.id = :categoryId)
        """)
    List<Product> search(@Param("name") String name, @Param("categoryId") Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
        SELECT p.id AS id, p.name AS name, p.barcode AS barcode, SUM(s.quantity - s.reservedQuantity) AS quantity FROM Product p
        JOIN Stock s ON p = s.product
        JOIN Location l ON l = s.location
        WHERE l.zone = :zone AND l.available = true
        GROUP BY p.id, p.name, p.barcode
        HAVING SUM(s.quantity - s.reservedQuantity) > 0
        """)
    List<ProductWithQuantityProjection> getProductsWithQuantities(
        @Param("zone") Zone zone
    );

    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    @Query("""
            SELECT p.id FROM Product p
            WHERE p.name = :name
        """)
    Optional<Long> findProductIdByName(
        @Param("name") String name
    );

    Optional<Product> findByBarcode(String barcode);
}
