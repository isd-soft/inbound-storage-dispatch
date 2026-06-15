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

    public List<Allocation> orderProcessesBySourceLocation(List<Allocation> allocations) {
        List<Allocation> sortedAllocations = new ArrayList<>(allocations);
        sortedAllocations.sort(Comparator.comparing(Allocation::getCreatedAt).thenComparing(Allocation::getId));

        Map<Long, List<Allocation>> groupedByLocation = new LinkedHashMap<>();
        for (Allocation allocation : sortedAllocations) {
            Long locationId = allocation.getStock().getLocation().getId();
            groupedByLocation.computeIfAbsent(locationId, ignored -> new ArrayList<>()).add(process);
        }

        return groupedByLocation.values().stream()
            .flatMap(List::stream)
            .toList();
    }

    public Optional<Allocation> findCurrentExecutableProcess(List<Allocation> allocations) {
        return orderProcessesBySourceLocation(allocations).stream()
            .filter(process -> allocation.getStatus() == Status.ASSIGNED || allocation.getStatus() == Status.IN_PROGRESS)
            .findFirst();
    }

    public Optional<Allocation> findNextExecutableProcessAfter(List<Allocation> allocations, Allocation completedAllocation) {
        List<Allocation> orderedAllocations = orderProcessesBySourceLocation(allocations);
        int completedIndex = orderedAllocations.indexOf(completedAllocation);

        if (completedIndex < 0) {
            return findCurrentExecutableProcess(allocations);
        }

        for (int index = completedIndex + 1; index < orderedAllocations.size(); index++) {
            Allocation allocation = orderedAllocations.get(index);
            if (allocation.getStatus() == Status.ASSIGNED || allocation.getStatus() == Status.IN_PROGRESS) {
                return Optional.of(process);
            }
        }

        return Optional.empty();
    }
}
