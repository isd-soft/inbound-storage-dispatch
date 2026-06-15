package com.isd.wms.service;

import com.isd.wms.entity.Process;
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

    public List<Process> orderProcessesBySourceLocation(List<Process> processes) {
        List<Process> sortedProcesses = new ArrayList<>(processes);
        sortedProcesses.sort(Comparator.comparing(Process::getCreatedAt).thenComparing(Process::getId));

        Map<Long, List<Process>> groupedByLocation = new LinkedHashMap<>();
        for (Process process : sortedProcesses) {
            Long locationId = process.getStock().getLocation().getId();
            groupedByLocation.computeIfAbsent(locationId, ignored -> new ArrayList<>()).add(process);
        }

        return groupedByLocation.values().stream()
            .flatMap(List::stream)
            .toList();
    }

    public Optional<Process> findCurrentExecutableProcess(List<Process> processes) {
        return orderProcessesBySourceLocation(processes).stream()
            .filter(process -> process.getStatus() == Status.ASSIGNED || process.getStatus() == Status.IN_PROGRESS)
            .findFirst();
    }

    public Optional<Process> findNextExecutableProcessAfter(List<Process> processes, Process completedProcess) {
        List<Process> orderedProcesses = orderProcessesBySourceLocation(processes);
        int completedIndex = orderedProcesses.indexOf(completedProcess);

        if (completedIndex < 0) {
            return findCurrentExecutableProcess(processes);
        }

        for (int index = completedIndex + 1; index < orderedProcesses.size(); index++) {
            Process process = orderedProcesses.get(index);
            if (process.getStatus() == Status.ASSIGNED || process.getStatus() == Status.IN_PROGRESS) {
                return Optional.of(process);
            }
        }

        return Optional.empty();
    }
}
