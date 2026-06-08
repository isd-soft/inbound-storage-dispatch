package com.isd.wms.entity;

import com.isd.wms.enums.ReplenishmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "replenishments")
public class Replenishment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "replenishment_seq")
    @SequenceGenerator(name = "replenishment_seq", sequenceName = "replenishments_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @NonNull
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @NonNull
    private ReplenishmentStatus status = ReplenishmentStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private Location destinationLocation;

    public Replenishment(Task task, Product product, Integer requestedQuantity, ReplenishmentStatus status, Location destinationLocation) {
        this.task = task;
        this.product = product;
        this.requestedQuantity = requestedQuantity;
        this.status = status;
        this.destinationLocation = destinationLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Replenishment that = (Replenishment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
