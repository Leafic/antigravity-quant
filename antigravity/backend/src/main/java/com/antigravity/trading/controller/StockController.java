package com.antigravity.trading.controller;

import com.antigravity.trading.domain.entity.StockMaster;
import com.antigravity.trading.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockSearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<List<StockMaster>> search(@RequestParam String query) {
        return ResponseEntity.ok(searchService.search(query));
    }
}
