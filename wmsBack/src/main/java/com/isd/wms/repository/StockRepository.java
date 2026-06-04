package com.isd.wms.repository;

import com.isd.wms.entity.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductIdAndSkuIgnoreCaseAndLocationId(Long productId, String sku, Long locationId);
}
