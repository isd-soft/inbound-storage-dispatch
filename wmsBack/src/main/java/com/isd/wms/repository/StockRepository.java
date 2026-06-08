package com.isd.wms.repository;

import com.isd.wms.entity.Stock;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND (s.quantity - s.reservedQuantity) > 0 ORDER BY (s.quantity - s.reservedQuantity) DESC")
    List<Stock> findAvailableStocksByProductId(@Param("productId") Long productId);

    Optional<Stock> findByProductIdAndSkuIgnoreCaseAndLocationId(Long productId, String sku, Long locationId);
}
