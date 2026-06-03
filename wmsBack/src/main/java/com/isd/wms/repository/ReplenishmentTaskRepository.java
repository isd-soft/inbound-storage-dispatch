package com.isd.wms.repository;

import com.isd.wms.entity.ReplenishmentTask;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ReplenishmentTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplenishmentTaskRepository extends JpaRepository<ReplenishmentTask, Long> {

    List<ReplenishmentTask> findReplenishmentTasksByOperator(User operator);

    List<ReplenishmentTask> findReplenishmentTasksByStatus(ReplenishmentTaskStatus status);
}
