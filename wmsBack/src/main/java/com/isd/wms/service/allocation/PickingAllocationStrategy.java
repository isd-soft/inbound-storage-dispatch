package com.isd.wms.service.allocation;

import com.isd.wms.entity.Stock;
import com.isd.wms.enums.TaskType;
import com.isd.wms.enums.Zone;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PickingAllocationStrategy implements StockAllocationStrategy {

    @Override
    public boolean support(TaskType taskType) {
        return taskType == TaskType.PICKING_ORDER;
    }

    @Override
    public Zone getSourceZone() {
        return Zone.PICKING;
    }

    @Override
    public void sortStocks(List<Stock> availableStocks) {
        availableStocks.sort(Comparator.comparingInt(
            (Stock s) -> s.getQuantity() - s.getReservedQuantity()
        ));
    }
}
