package com.ishan.portfolio_risk_model.controller;

import com.ishan.portfolio_risk_model.dto.HoldingsRequest;
import com.ishan.portfolio_risk_model.dto.HoldingsResponse;
import com.ishan.portfolio_risk_model.service.HoldingService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST contorller for all holdings requests
@RestController
@RequestMapping("/api/v1/holdings")
@AllArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;

    // get all holdings
    @GetMapping
    public ResponseEntity<List<HoldingsResponse>> getHoldings() {
        List<HoldingsResponse> holdings = holdingService.getHoldings();
        return ResponseEntity.ok(holdings);
    }

    // create a new holding
    @PostMapping
    public ResponseEntity<HoldingsResponse> createHolding(@Valid @RequestBody HoldingsRequest request) {
        HoldingsResponse holding = holdingService.createHolding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(holding);
    }

    // update a holding
    @PutMapping("/{id}")
    public ResponseEntity<HoldingsResponse> updateHolding(
            @PathVariable Long id,
            @Valid @RequestBody HoldingsRequest request) {

        HoldingsResponse holding = holdingService.updateHolding(id, request);
        return ResponseEntity.ok(holding);
    }

    // delete a holding
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHolding(@PathVariable Long id) {
        holdingService.deleteHolding(id);
        return ResponseEntity.noContent().build();
    }
}
