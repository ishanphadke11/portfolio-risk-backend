package com.ishan.portfolio_risk_model.service;

import com.ishan.portfolio_risk_model.dto.FlaskAnalysisRequest;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisResponse;
import com.ishan.portfolio_risk_model.dto.FlaskErrorResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// client that communicates with flask analysis service
@Component
@AllArgsConstructor
public class FlaskClient {

    private final RestClient flaskRestClient;

    public FlaskAnalysisResponse runFactorRegression(FlaskAnalysisRequest request) {
        try {
            return flaskRestClient.post()
                    .uri("/api/analysis/factor-regression")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange(((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().is2xxSuccessful()) {
                            return clientResponse.bodyTo(FlaskAnalysisResponse.class);
                        }

                        FlaskErrorResponse errorResponse = clientResponse.bodyTo(FlaskErrorResponse.class);
                        String errorMessage = (errorResponse != null && errorResponse.getError() != null)
                                ? errorResponse.getError()
                                : "Flask service returned status " + clientResponse.getStatusCode();

                        HttpStatus status = mapFlaskStatus(clientResponse.getStatusCode().value());
                        throw new FlaskServiceException(errorMessage, status);
                    }));
        } catch (FlaskServiceException e) {
            throw e;
        } catch (RestClientException e) {
            throw new FlaskServiceException(
                    "Factor analysis service is unavailable: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private HttpStatus mapFlaskStatus(int flaskStatus) {
        return switch (flaskStatus) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
