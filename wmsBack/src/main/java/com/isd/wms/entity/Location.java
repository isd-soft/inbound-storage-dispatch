package com.isd.wms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE , generator = "location_seq")
    @SequenceGenerator(name = "location_seq", sequenceName = "locations_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "location_code", nullable = false, unique = true, length = 50)
    private String locationCode;

    @Column(length = 50)
    private String zone;

    @Column
    private String description;

    @Column(nullable = false)
    private Boolean available = true;
}