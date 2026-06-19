package com.isd.wms.repository;

import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.enums.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductAndLocation(Product product, Location location);

    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND (s.quantity - s.reservedQuantity) > 0 ORDER BY (s.quantity - s.reservedQuantity) DESC")
    List<Stock> findAvailableStocksByProductId(@Param("productId") Long productId);

    Optional<Stock> findByProductIdAndLocationId(Long productId, Long locationId);

    boolean existsByLocationIdAndQuantityGreaterThan(Long locationId, Integer quantity);

    @Query("""
            SELECT s FROM Stock s
            WHERE s.product.id = :productId
              AND s.location.zone = :zone
              AND (s.quantity - s.reservedQuantity) > 0
            """)
    List<Stock> findAvailableStocksByProductIdAndZone(@Param("productId") Long productId, @Param("zone") Zone zone);

    Optional<Stock> findByLocationId(Long locationId);
}
