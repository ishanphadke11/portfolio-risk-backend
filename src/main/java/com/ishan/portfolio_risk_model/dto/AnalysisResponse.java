package com.ishan.portfolio_risk_model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

// response body of what is sent to the frontend
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AnalysisResponse {

    private Long id;
    private LocalDateTime analysisDate;
    private BigDecimal alpha;
    private BigDecimal betaMkt;
    private BigDecimal betaSmb;
    private BigDecimal betaHml;
    private BigDecimal betaRmw;
    private BigDecimal betaCma;
    private BigDecimal rSquared;
    private Map<String, BigDecimal> tStats;
}
