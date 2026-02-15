package com.ishan.portfolio_risk_model.service;

import com.ishan.portfolio_risk_model.domain.entity.FactorAnalysisResultsEntity;
import com.ishan.portfolio_risk_model.domain.entity.HoldingsEntity;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import com.ishan.portfolio_risk_model.domain.repository.FactorAnalysisResultsRepository;
import com.ishan.portfolio_risk_model.domain.repository.HoldingsRepository;
import com.ishan.portfolio_risk_model.dto.AnalysisResponse;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisRequest;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class AnalysisService {

    private final HoldingsRepository holdingsRepository;
    private final FactorAnalysisResultsRepository resultsRepository;
    private final FlaskClient flaskClient;

    // run factor analysis on the user's portfolio
    public AnalysisResponse runAnalysis(LocalDate startDate, LocalDate endDate) {
        UserEntity user = getCurrentUser();

        List<HoldingsEntity> holdings = holdingsRepository.findByUser(user);
        if (holdings.isEmpty()) {
            throw new IllegalArgumentException("No holdings found. Add holdings before running analysis.");
        }

        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusYears(3);
        }

        FlaskAnalysisRequest request = buildFlaskRequest(holdings, startDate, endDate);
        FlaskAnalysisResponse flaskResponse = flaskClient.runFactorRegression(request);

        FactorAnalysisResultsEntity entity = new FactorAnalysisResultsEntity();
        entity.setUser(user);
        entity.setAnalysisDate(LocalDateTime.now());
        entity.setAlpha(flaskResponse.getAlpha());
        entity.setBetaMkt(flaskResponse.getBetaMkt());
        entity.setBetaSmb(flaskResponse.getBetaSmb());
        entity.setBetaHml(flaskResponse.getBetaHml());
        entity.setBetaRmw(flaskResponse.getBetaRmw());
        entity.setBetaCma(flaskResponse.getBetaCma());
        entity.setRSquared(flaskResponse.getRSquared());
        
        FactorAnalysisResultsEntity saved = resultsRepository.save(entity);
        
        return toResponse(saved, flaskResponse.getTStats());
    }

    public List<AnalysisResponse> getHistory() {
        UserEntity user = getCurrentUser();
        return resultsRepository.findByUserOrderByAnalysisDateDesc(user).stream()
                .map(entity -> toResponse(entity, null))
                .toList();
    }

    public AnalysisResponse getAnalysisById(Long id) {
        UserEntity user = getCurrentUser();
        FactorAnalysisResultsEntity entity = resultsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis result not found"));

        if (!entity.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Analysis result not found");
        }

        return toResponse(entity, null);
    }

    private AnalysisResponse toResponse(FactorAnalysisResultsEntity entity, Map<String, BigDecimal> tStats) {
        return new AnalysisResponse(
                entity.getId(),
                entity.getAnalysisDate(),
                entity.getAlpha(),
                entity.getBetaMkt(),
                entity.getBetaSmb(),
                entity.getBetaHml(),
                entity.getBetaRmw(),
                entity.getBetaCma(),
                entity.getRSquared(),
                tStats
        );
    }

    private FlaskAnalysisRequest buildFlaskRequest(List<HoldingsEntity> holdings, LocalDate startDate, LocalDate endDate) {
        List<FlaskAnalysisRequest.FlaskHolding> flaskHoldings = holdings.stream()
                .map(h -> new FlaskAnalysisRequest.FlaskHolding(
                        h.getTicker(),
                        h.getQuantity().intValue()
                ))
                .toList();

        return new FlaskAnalysisRequest(
                flaskHoldings,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }

    private UserEntity getCurrentUser() {
        return (UserEntity) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
