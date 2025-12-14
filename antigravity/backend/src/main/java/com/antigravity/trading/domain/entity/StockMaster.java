package com.antigravity.trading.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_master")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMaster {

    @Id
    @Column(length = 10)
    private String code;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String market; // KOSPI, KOSDAQ, KONEX

    @Column(length = 50)
    private String sector;

    @Column(length = 50)
    private String industry;

    @Column(length = 8)
    private String listingDate; // YYYYMMDD

    @Column
    private Boolean isManaged;

    @Column
    private Boolean isSuspended;

    @Column
    private Boolean isFavorite;

    @Column
    private LocalDateTime lastUpdated;
}
