package com.isd.wms.entity;

import com.isd.wms.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order extends BaseTimestampEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_gen")
    @SequenceGenerator(name = "order_gen", sequenceName = "orders_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "logic_id", nullable = false, unique = true)
    private String logicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();

    public void addOrderLine(OrderLine line) {
        orderLines.add(line);
//        line.setOrder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Order order = (Order) o;
        return id != null && Objects.equals(id, order.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}