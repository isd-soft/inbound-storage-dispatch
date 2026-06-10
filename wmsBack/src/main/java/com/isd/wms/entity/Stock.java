package com.isd.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.Hibernate;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_seq")
    @SequenceGenerator(name = "stock_seq", sequenceName = "stocks_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "SKU", nullable = false, length = 100)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Version
    private Long version;

    public void removeQuantity(int quantityToMove) {
        this.quantity -= quantityToMove;
        this.reservedQuantity -= quantityToMove;
    }

    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Stock stock = (Stock) o;
        return id != null && Objects.equals(id, stock.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}