package com.isd.wms.entity;

import com.isd.wms.enums.InventoryOperationType;
import com.isd.wms.enums.InventoryAdjustmentReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

@Entity
@Table(name = "inventory_history")
@Getter
@Setter
@NoArgsConstructor
public class InventoryHistory{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_history_seq")
    @SequenceGenerator(name = "inventory_history_seq", sequenceName = "inventory_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 100)
    private String barcode;

    @Column(name = "altered_quantity", nullable = false)
    private Integer alteredQuantity;

    @Column(name = "quantity_after_change", nullable = false)
    private Integer quantityAfterChange;

    @Column(name = "previous_quantity")
    private Integer previousQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    private Location sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private Location destinationLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 50)
    private InventoryOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_reason", length = 50)
    private InventoryAdjustmentReason adjustmentReason;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public InventoryHistory(Product product, String barcode, Integer alteredQuantity, Integer quantityAfterChange, Integer previousQuantity, Location sourceLocation, Location destinationLocation, InventoryOperationType operationType, InventoryAdjustmentReason adjustmentReason, String comment, User user) {
        this.product = product;
        this.barcode = barcode;
        this.alteredQuantity = alteredQuantity;
        this.quantityAfterChange = quantityAfterChange;
        this.previousQuantity = previousQuantity;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
        this.operationType = operationType;
        this.adjustmentReason = adjustmentReason;
        this.comment = comment;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        InventoryHistory that = (InventoryHistory) o;
        return id != null && Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
