package com.antigravity.trading.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_master")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMaster {

    @Id
    private String code;

    private String name;
    private String market; // KOSPI, KOSDAQ
    private String sector;
}
