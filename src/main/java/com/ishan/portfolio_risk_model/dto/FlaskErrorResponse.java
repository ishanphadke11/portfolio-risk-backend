package com.ishan.portfolio_risk_model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// response body for error returned by the flaks service
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class FlaskErrorResponse {
    private String error;
    private String code;
}
