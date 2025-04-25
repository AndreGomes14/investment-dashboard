package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.dto.investment.InstrumentSearchResult;
import com.myapp.investment_dashboard_backend.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final MarketDataService marketDataService;

    @Autowired
    public SearchController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/instruments")
    public ResponseEntity<List<InstrumentSearchResult>> searchInstruments(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        List<InstrumentSearchResult> results = marketDataService.searchInstruments(query);
        return ResponseEntity.ok(results);
    }
}