package com.isd.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
public class Stock extends BaseTimestampEntity {

    public Stock(Long id, Product product, Location location, Integer quantity, Integer reservedQuantity, LocalDate manufactureDate, LocalDate expirationDate, Long version) {
        this.id = id;
        this.product = product;
        this.location = location;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.manufactureDate = manufactureDate;
        this.expirationDate = expirationDate;
        this.version = version;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_seq")
    @SequenceGenerator(name = "stock_seq", sequenceName = "stocks_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @Getter(AccessLevel.NONE)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Min(0)
    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Version
    private Long version;

    public Stock(Product product, Location location) {
        this.product = product;
        this.location = location;
    }

    public Stock(Product product, Location location, Integer quantity, LocalDate manufactureDate, LocalDate expirationDate) {
        this.product = product;
        this.location = location;
        this.quantity = quantity;
        this.manufactureDate = manufactureDate;
        this.expirationDate = expirationDate;
    }

    public Optional<Product> getProduct() {
        return Optional.ofNullable(product);
    }

    public void removeQuantity(int quantityToMove) {
        this.quantity -= quantityToMove;
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantityToMove);
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

    public void updateDate(LocalDate manufactureDate, LocalDate expirationDate) {
        this.manufactureDate = manufactureDate.isAfter(this.manufactureDate)
                ? manufactureDate : this.manufactureDate;

        this.expirationDate = expirationDate.isAfter(this.expirationDate)
                ? expirationDate : this.expirationDate;
    }
}
