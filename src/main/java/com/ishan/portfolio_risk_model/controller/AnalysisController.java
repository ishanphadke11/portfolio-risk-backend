package com.ishan.portfolio_risk_model.controller;

import com.ishan.portfolio_risk_model.dto.AnalysisResponse;
import com.ishan.portfolio_risk_model.service.AnalysisService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@AllArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // run analysis on current user's portfolio
    @PostMapping("/run")
    public ResponseEntity<AnalysisResponse> runAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
            ) {
        AnalysisResponse analysisResponse = analysisService.runAnalysis(startDate, endDate);
        return ResponseEntity.ok(analysisResponse);
    }

    // get past analysis results for user
    @GetMapping("/history")
    public ResponseEntity<List<AnalysisResponse>> getHistory() {
        List<AnalysisResponse> history = analysisService.getHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponse> getAnalysisById(@PathVariable Long id) {
        AnalysisResponse response = analysisService.getAnalysisById(id);
        return ResponseEntity.ok(response);
    }
}
