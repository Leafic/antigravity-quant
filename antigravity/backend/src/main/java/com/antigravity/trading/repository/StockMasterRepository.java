package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.StockMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMasterRepository extends JpaRepository<StockMaster, String> {

    @Query("SELECT s FROM StockMaster s WHERE s.name LIKE %:query% OR s.code LIKE %:query%")
    List<StockMaster> search(@Param("query") String query, Pageable pageable);
}
