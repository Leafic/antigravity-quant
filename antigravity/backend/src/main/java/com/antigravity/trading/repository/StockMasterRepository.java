package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.StockMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMasterRepository extends JpaRepository<StockMaster, String> {

    @Query("SELECT s FROM StockMaster s WHERE s.name LIKE %:query% OR s.code LIKE %:query%")
    List<StockMaster> search(@Param("query") String query, Pageable pageable);

    // 페이징 지원 검색
    @Query("SELECT s FROM StockMaster s WHERE " +
           "(:query IS NULL OR s.name LIKE %:query% OR s.code LIKE %:query%) AND " +
           "(:market IS NULL OR s.market = :market) AND " +
           "(:favoriteOnly = false OR s.isFavorite = true)")
    Page<StockMaster> searchWithFilters(
        @Param("query") String query,
        @Param("market") String market,
        @Param("favoriteOnly") boolean favoriteOnly,
        Pageable pageable
    );

    // 즐겨찾기 종목만 조회
    List<StockMaster> findByIsFavoriteTrueOrderByNameAsc();

    // 시장별 종목 수 조회
    @Query("SELECT COUNT(s) FROM StockMaster s WHERE s.market = :market")
    long countByMarket(@Param("market") String market);
}
