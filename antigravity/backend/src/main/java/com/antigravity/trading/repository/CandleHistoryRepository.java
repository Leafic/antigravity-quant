package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.CandleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CandleHistoryRepository extends JpaRepository<CandleHistory, Long> {
    List<CandleHistory> findBySymbolAndTimeBetween(String symbol, LocalDateTime start, LocalDateTime end);

    @Query("SELECT MIN(c.time) FROM CandleHistory c WHERE c.symbol = :symbol")
    LocalDateTime findMinTimeBySymbol(@Param("symbol") String symbol);

    @Query("SELECT MAX(c.time) FROM CandleHistory c WHERE c.symbol = :symbol")
    LocalDateTime findMaxTimeBySymbol(@Param("symbol") String symbol);

    // 특정 종목의 저장된 날짜 목록 조회 (중복 체크용)
    @Query("SELECT DISTINCT CAST(c.time AS LocalDate) FROM CandleHistory c WHERE c.symbol = :symbol")
    Set<LocalDate> findDistinctDatesBySymbol(@Param("symbol") String symbol);

    // 특정 종목, 특정 날짜의 데이터 존재 여부 확인
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CandleHistory c WHERE c.symbol = :symbol AND CAST(c.time AS LocalDate) = :date")
    boolean existsBySymbolAndDate(@Param("symbol") String symbol, @Param("date") LocalDate date);

    // 특정 종목의 데이터 개수 조회
    @Query("SELECT COUNT(c) FROM CandleHistory c WHERE c.symbol = :symbol")
    long countBySymbol(@Param("symbol") String symbol);

    // 특정 종목, 특정 기간의 데이터 삭제
    @Modifying
    @Query("DELETE FROM CandleHistory c WHERE c.symbol = :symbol AND c.time BETWEEN :start AND :end")
    void deleteBySymbolAndTimeBetween(@Param("symbol") String symbol, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 특정 날짜의 데이터 조회
    Optional<CandleHistory> findBySymbolAndTime(String symbol, LocalDateTime time);
}
