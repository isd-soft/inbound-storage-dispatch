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
@Table(name = "allocations")
public class Allocation extends BaseTimestampEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_gen")
    @SequenceGenerator(name = "process_gen", sequenceName = "processes_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private Integer quantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "source_location_scanned", nullable = false)
    private boolean sourceLocationScanned = false;

    @Column(name = "product_scanned", nullable = false)
    private boolean productScanned = false;

    @Column(name = "picked_quantity")
    private Integer pickedQuantity;

    public Allocation(Task task, Stock stock, Integer quantity, Status status) {
        this.task = task;
        this.stock = stock;
        this.quantity = quantity;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Allocation allocation = (Allocation) o;
        return id != null && Objects.equals(id, allocation.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
