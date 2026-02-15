package com.ishan.portfolio_risk_model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Request body for creating/updating a holding
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HoldingsRequest {

    @NotBlank(message = "Ticker is required")
    @Size(max = 10, message = "Ticker must be 10 characters or less")
    private String ticker;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
}
