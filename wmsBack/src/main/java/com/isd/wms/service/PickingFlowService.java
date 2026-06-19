package com.isd.wms.service;

import com.isd.wms.entity.Allocation;
import com.isd.wms.enums.Status;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PickingFlowService {

    public List<Allocation> orderAllocationsBySourceLocation(List<Allocation> allocations) {
        List<Allocation> sortedAllocations = new ArrayList<>(allocations);
        sortedAllocations.sort(Comparator.comparing(Allocation::getCreatedAt).thenComparing(Allocation::getId));

        Map<Long, List<Allocation>> groupedByLocation = new LinkedHashMap<>();
        for (Allocation allocation : sortedAllocations) {
            Long locationId = allocation.getStock().getLocation().getId();
            groupedByLocation.computeIfAbsent(locationId, ignored -> new ArrayList<>()).add(allocation);
        }

        return groupedByLocation.values().stream()
            .flatMap(List::stream)
            .toList();
    }

    public Optional<Allocation> findCurrentExecutableAllocation(List<Allocation> allocations) {
        return orderAllocationsBySourceLocation(allocations).stream()
            .filter(allocation -> allocation.getStatus() == Status.CREATED
                || allocation.getStatus() == Status.ASSIGNED
                || allocation.getStatus() == Status.IN_PROGRESS)
            .findFirst();
    }

    public Optional<Allocation> findNextExecutableAllocationAfter(List<Allocation> allocations, Allocation completedAllocation) {
        List<Allocation> orderedAllocations = orderAllocationsBySourceLocation(allocations);
        int completedIndex = orderedAllocations.indexOf(completedAllocation);

        if (completedIndex < 0) {
            return findCurrentExecutableAllocation(allocations);
        }

        for (int index = completedIndex + 1; index < orderedAllocations.size(); index++) {
            Allocation allocation = orderedAllocations.get(index);
            if (allocation.getStatus() == Status.CREATED
                || allocation.getStatus() == Status.ASSIGNED
                || allocation.getStatus() == Status.IN_PROGRESS) {
                return Optional.of(allocation);
            }
        }

        return Optional.empty();
    }
}
