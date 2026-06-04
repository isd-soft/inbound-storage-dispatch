package com.isd.wms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

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

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Version
    private Long version;

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
