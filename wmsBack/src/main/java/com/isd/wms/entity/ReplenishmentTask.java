package com.isd.wms.entity;

import com.isd.wms.enums.ReplenishmentTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "replenishment_tasks")
public class ReplenishmentTask {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "replenishment_task_seq")
    @SequenceGenerator(name = "replenishment_task_seq", sequenceName = "replenishment_tasks_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @NonNull
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @NonNull
    private ReplenishmentTaskStatus status = ReplenishmentTaskStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    private Location sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private Location destinationLocation;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Timestamp createdAt;

    public ReplenishmentTask(Product product, Integer requestedQuantity, ReplenishmentTaskStatus status, Location sourceLocation, Location destinationLocation) {
        this.product = product;
        this.requestedQuantity = requestedQuantity;
        this.status = status;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ReplenishmentTask that = (ReplenishmentTask) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
