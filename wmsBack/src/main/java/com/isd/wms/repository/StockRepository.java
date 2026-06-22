package com.isd.wms.repository;

import com.isd.wms.entity.Stock;
import com.isd.wms.enums.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Stock} entities.
 * <p>
 * Provides methods for finding stock by product, location, zone, and checking
 * availability. Also includes queries to find stocks with positive available
 * quantity, sorted by availability.
 * </p>
 */
public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Finds all stocks for a given product that have positive available quantity
     * (quantity - reservedQuantity > 0), ordered by available quantity descending.
     *
     * @param productId the product ID
     * @return list of stocks
     */
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND (s.quantity - s.reservedQuantity) > 0 ORDER BY (s.quantity - s.reservedQuantity) DESC")
    List<Stock> findAvailableStocksByProductId(@Param("productId") Long productId);


    /**
     * Finds stock for a specific product and location by their IDs.
     *
     * @param productId  the product ID
     * @param locationId the location ID
     * @return an Optional containing the stock, if found
     */
    Optional<Stock> findByProductIdAndLocationId(Long productId, Long locationId);

    /**
     * Checks whether a location has any stock with quantity greater than the given value.
     *
     * @param locationId the location ID
     * @param quantity   the threshold (usually 0)
     * @return true if there is stock with quantity > threshold
     */
    boolean existsByLocationIdAndQuantityGreaterThan(Long locationId, Integer quantity);

    /**
     * Finds stocks for a product in a specific zone with positive available quantity.
     *
     * @param productId the product ID
     * @param zone      the zone (e.g., REPLENISHMENT, PICKING)
     * @return list of stocks
     */
    @Query("""
        SELECT s FROM Stock s
        WHERE s.product.id = :productId
          AND s.location.zone = :zone
          AND (s.quantity - s.reservedQuantity) > 0
        """)
    List<Stock> findAvailableStocksByProductIdAndZone(@Param("productId") Long productId, @Param("zone") Zone zone);

    /**
     * Finds the stock located at a given location.
     *
     * @param locationId the location ID
     * @return an Optional containing the stock, if found
     */
    Optional<Stock> findByLocationId(Long locationId);
}
