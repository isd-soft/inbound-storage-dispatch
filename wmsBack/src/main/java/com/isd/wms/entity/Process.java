package com.isd.wms.entity;

import com.isd.wms.enums.ProcessStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import java.util.Objects;
import java.util.Optional;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "processes")
public class Process extends BaseTimestampEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_gen")
    @SequenceGenerator(name = "process_gen", sequenceName = "processes_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessStatus status;

    @Column(name = "source_location_scanned", nullable = false)
    private boolean sourceLocationScanned = false;

    @Column(name = "product_scanned", nullable = false)
    private boolean productScanned = false;

    @Column(name = "picked_quantity")
    private Integer pickedQuantity;

    public Optional<User> getOperator() {
        return Optional.ofNullable(operator);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Process process = (Process) o;
        return id != null && Objects.equals(id, process.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
