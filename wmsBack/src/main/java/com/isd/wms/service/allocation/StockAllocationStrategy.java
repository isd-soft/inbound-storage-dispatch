package com.isd.wms.service.allocation;

import com.isd.wms.entity.Stock;
import com.isd.wms.enums.TaskType;
import com.isd.wms.enums.Zone;

import java.util.List;

public interface StockAllocationStrategy {

    boolean support(TaskType taskType);

    Zone getSourceZone();

    void sortStocks(List<Stock> availableStocks);
}
