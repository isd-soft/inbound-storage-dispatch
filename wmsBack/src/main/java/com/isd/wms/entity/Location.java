package com.isd.wms.entity;

import com.isd.wms.enums.Zone;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE , generator = "location_seq")
    @SequenceGenerator(name = "location_seq", sequenceName = "locations_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "location_code", nullable = false, unique = true, length = 50)
    private String locationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone")
    private Zone zone;

    @Column
    private String description;

    @Column(nullable = false)
    private Boolean available = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Location location = (Location) o;
        return id != null && Objects.equals(id, location.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}