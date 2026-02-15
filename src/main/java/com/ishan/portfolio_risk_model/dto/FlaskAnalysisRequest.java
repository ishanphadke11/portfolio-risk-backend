package com.ishan.portfolio_risk_model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// request body sent to the flask factor analysis service
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FlaskAnalysisRequest {

    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
    @Getter
    public static class FlaskHolding {
        private String ticker;
        private int quantity;
    }

    private List<FlaskHolding> holdings;
    private String startDate;
    private String endDate;
}
