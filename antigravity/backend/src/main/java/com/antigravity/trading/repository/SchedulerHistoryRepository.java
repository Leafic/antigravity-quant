package com.antigravity.trading.repository;

import com.antigravity.trading.domain.entity.SchedulerHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulerHistoryRepository extends JpaRepository<SchedulerHistory, Long> {

    // 최근 실행 히스토리 조회
    List<SchedulerHistory> findByOrderByStartTimeDesc(Pageable pageable);

    // 특정 Job의 최근 실행 조회
    Optional<SchedulerHistory> findFirstByJobNameOrderByStartTimeDesc(String jobName);

    // 실행 중인 Job 조회
    List<SchedulerHistory> findByStatus(String status);

    // 기간별 조회
    List<SchedulerHistory> findByStartTimeBetweenOrderByStartTimeDesc(
        LocalDateTime start, LocalDateTime end);

    // 통계: 성공률
    @Query("SELECT COUNT(s) FROM SchedulerHistory s WHERE s.jobName = :jobName AND s.status = 'SUCCESS'")
    long countSuccessByJobName(String jobName);

    @Query("SELECT COUNT(s) FROM SchedulerHistory s WHERE s.jobName = :jobName")
    long countTotalByJobName(String jobName);
}
