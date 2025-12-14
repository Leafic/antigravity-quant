package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.StockMaster;
import com.antigravity.trading.repository.StockMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 종목 마스터 데이터 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/stock-master")
@RequiredArgsConstructor
public class StockMasterController {

    private final StockMasterRepository stockMasterRepository;

    /**
     * 종목 목록 조회 (페이징, 검색, 필터링)
     * GET /api/stock-master?page=0&size=20&query=삼성&market=KOSPI&favoriteOnly=false
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "false") boolean favoriteOnly,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        Sort sort = sortOrder.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<StockMaster> stockPage = stockMasterRepository.searchWithFilters(
            query, market, favoriteOnly, pageable
        );

        Map<String, Object> response = new HashMap<>();
        response.put("content", stockPage.getContent());
        response.put("currentPage", stockPage.getNumber());
        response.put("totalItems", stockPage.getTotalElements());
        response.put("totalPages", stockPage.getTotalPages());
        response.put("size", stockPage.getSize());
        response.put("hasNext", stockPage.hasNext());
        response.put("hasPrevious", stockPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * 종목 상세 조회
     * GET /api/stock-master/{code}
     */
    @GetMapping("/{code}")
    public ResponseEntity<StockMaster> getStock(@PathVariable String code) {
        Optional<StockMaster> stock = stockMasterRepository.findById(code);
        return stock.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 즐겨찾기 토글
     * POST /api/stock-master/{code}/favorite
     */
    @PostMapping("/{code}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable String code) {
        log.info("Toggling favorite for stock: {}", code);

        Optional<StockMaster> stockOpt = stockMasterRepository.findById(code);

        if (stockOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "종목을 찾을 수 없습니다: " + code);
            return ResponseEntity.notFound().build();
        }

        StockMaster stock = stockOpt.get();
        boolean newFavoriteStatus = stock.getIsFavorite() == null || !stock.getIsFavorite();
        stock.setIsFavorite(newFavoriteStatus);
        stock.setLastUpdated(LocalDateTime.now());
        stockMasterRepository.save(stock);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isFavorite", newFavoriteStatus);
        response.put("message", newFavoriteStatus ? "즐겨찾기에 추가되었습니다" : "즐겨찾기에서 제거되었습니다");

        return ResponseEntity.ok(response);
    }

    /**
     * 즐겨찾기 종목 목록 조회
     * GET /api/stock-master/favorites
     */
    @GetMapping("/favorites")
    public ResponseEntity<List<StockMaster>> getFavorites() {
        List<StockMaster> favorites = stockMasterRepository.findByIsFavoriteTrueOrderByNameAsc();
        return ResponseEntity.ok(favorites);
    }

    /**
     * 통계 조회
     * GET /api/stock-master/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalCount = stockMasterRepository.count();
        long kospiCount = stockMasterRepository.countByMarket("KOSPI");
        long kosdaqCount = stockMasterRepository.countByMarket("KOSDAQ");
        long konexCount = stockMasterRepository.countByMarket("KONEX");
        long favoriteCount = stockMasterRepository.findByIsFavoriteTrueOrderByNameAsc().size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("kospiCount", kospiCount);
        stats.put("kosdaqCount", kosdaqCount);
        stats.put("konexCount", konexCount);
        stats.put("favoriteCount", favoriteCount);

        return ResponseEntity.ok(stats);
    }

    /**
     * 종목 삭제 (관리자 기능)
     * DELETE /api/stock-master/{code}
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<Map<String, Object>> deleteStock(@PathVariable String code) {
        log.info("Deleting stock: {}", code);

        if (!stockMasterRepository.existsById(code)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "종목을 찾을 수 없습니다");
            return ResponseEntity.notFound().build();
        }

        stockMasterRepository.deleteById(code);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "종목이 삭제되었습니다");

        return ResponseEntity.ok(response);
    }

    /**
     * 종목 수동 추가/수정 (관리자 기능)
     * PUT /api/stock-master/{code}
     */
    @PutMapping("/{code}")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable String code,
            @RequestBody StockMaster stock) {

        log.info("Updating stock: {}", code);

        stock.setCode(code);
        stock.setLastUpdated(LocalDateTime.now());
        StockMaster saved = stockMasterRepository.save(stock);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "종목 정보가 저장되었습니다");
        response.put("stock", saved);

        return ResponseEntity.ok(response);
    }
}
