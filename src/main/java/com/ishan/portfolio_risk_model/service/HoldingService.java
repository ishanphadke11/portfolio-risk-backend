package com.ishan.portfolio_risk_model.service;

import com.ishan.portfolio_risk_model.domain.entity.HoldingsEntity;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import com.ishan.portfolio_risk_model.domain.repository.HoldingsRepository;
import com.ishan.portfolio_risk_model.dto.HoldingsRequest;
import com.ishan.portfolio_risk_model.dto.HoldingsResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

// service for managing user holdings
@Service
@AllArgsConstructor
public class HoldingService {

    private final HoldingsRepository holdingsRepository;

    // get all holdings
    public List<HoldingsResponse> getHoldings() {
        UserEntity user = getCurrentUser();

        return holdingsRepository.findByUser(user).stream()
                .map(this::toResponse).toList();
    }

    // create a new holding
    public HoldingsResponse createHolding(HoldingsRequest request) {

        UserEntity user = getCurrentUser();
        String ticker = request.getTicker();

        if (holdingsRepository.existsByUserAndTicker(user, ticker)) {
            throw new IllegalArgumentException("You already have a holding for " + ticker);
        }

        HoldingsEntity holding = new HoldingsEntity();
        holding.setUser(user);
        holding.setTicker(ticker);
        holding.setQuantity(request.getQuantity());

        HoldingsEntity saved = holdingsRepository.save(holding);
        return toResponse(saved);
    }

    // update the quantity of an existing holding
    public HoldingsResponse updateHolding(Long holdingId, HoldingsRequest request) {
        UserEntity user = getCurrentUser();

        // find the holding
        HoldingsEntity holding = holdingsRepository.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));

        // see if holding belongs to user
        if (!holding.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Holding not found");
        }

        // set new quantity
        holding.setQuantity(request.getQuantity());

        HoldingsEntity saved = holdingsRepository.save(holding);
        return toResponse(saved);
    }

    // delete a holding
    public void deleteHolding(Long holdingId) {
        UserEntity user = getCurrentUser();

        // find holding
        HoldingsEntity holding = holdingsRepository.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));

        // see if holding belongs to user
        if (!holding.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Holding not found");
        }
        holdingsRepository.delete(holding);
    }

    private HoldingsResponse toResponse(HoldingsEntity saved) {
        return new HoldingsResponse(
                saved.getId(),
                saved.getTicker(),
                saved.getQuantity()
        );
    }

    private UserEntity getCurrentUser() {
        return (UserEntity) SecurityContextHolder.getContext().
                getAuthentication().getPrincipal();
    }
}
