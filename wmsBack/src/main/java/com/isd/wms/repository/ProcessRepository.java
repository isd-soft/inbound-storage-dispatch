package com.isd.wms.repository;

import com.isd.wms.entity.Process;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessRepository extends JpaRepository<Process, Long> {
    List<Process> findAllByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);

    List<Process> findByStatus(ProcessStatus status);

    @Query("SELECT p FROM Process p WHERE p.operator = :operator AND p.status IN (:statuses)")
    List<Process> findByOperatorAndStatuses(User operator, List<ProcessStatus> statuses);
}