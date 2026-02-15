package com.ishan.portfolio_risk_model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

// response body recieved from the flask analysis service
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FlaskAnalysisResponse {

    private BigDecimal alpha;
    private BigDecimal betaMkt;
    private BigDecimal betaSmb;
    private BigDecimal betaHml;
    private BigDecimal betaRmw;
    private BigDecimal betaCma;
    private BigDecimal rSquared;
    private Map<String, BigDecimal> tStats;
}
