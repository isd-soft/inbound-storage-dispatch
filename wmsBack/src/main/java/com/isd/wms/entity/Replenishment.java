package com.isd.wms.entity;

import com.isd.wms.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "replenishments")
public class Replenishment extends BaseTimestampEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "replenishment_seq")
    @SequenceGenerator(name = "replenishment_seq", sequenceName = "replenishments_sequence", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "task_id", referencedColumnName = "id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @NonNull
    private Integer requestedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @NonNull
    private Status status = Status.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private Location destinationLocation;

    public Replenishment(Product product, Task task, Integer requestedQuantity, Status status, Location destinationLocation) {
        this.product = product;
        this.task = task;
        this.requestedQuantity = requestedQuantity;
        this.status = status;
        this.destinationLocation = destinationLocation;
    }

    public Replenishment(Task task, Product product, Integer requestedQuantity, Location destinationLocation) {
        this.task = task;
        this.product = product;
        this.requestedQuantity = requestedQuantity;
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
