package com.isd.wms.repository;

import com.isd.wms.entity.Process;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessRepository extends JpaRepository<Process, Long> {
    List<Process> findAllByTaskId(Long taskId);
    void deleteByTaskId(Long taskId);
}