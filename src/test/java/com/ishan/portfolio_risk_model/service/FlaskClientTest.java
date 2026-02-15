package com.ishan.portfolio_risk_model.service;

import com.ishan.portfolio_risk_model.dto.FlaskAnalysisRequest;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlaskClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RequestBodySpec requestBodySpec;

    private FlaskClient flaskClient;
    private FlaskAnalysisRequest testRequest;

    @BeforeEach
    void setUp() {
        flaskClient = new FlaskClient(restClient);

        testRequest = new FlaskAnalysisRequest(
                List.of(new FlaskAnalysisRequest.FlaskHolding("AAPL", 10)),
                "2022-01-01",
                "2025-01-01"
        );
    }

    @Test
    @DisplayName("Should return response on successful Flask call")
    void runFactorRegression_success_returnsResponse() {
        // Arrange
        FlaskAnalysisResponse expectedResponse = new FlaskAnalysisResponse();
        expectedResponse.setAlpha(new BigDecimal("0.000234"));
        expectedResponse.setBetaMkt(new BigDecimal("0.856420"));
        expectedResponse.setRSquared(new BigDecimal("0.876543"));
        expectedResponse.setTStats(Map.of("alpha", new BigDecimal("1.2345")));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/analysis/factor-regression")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(testRequest)).thenReturn(requestBodySpec);
        when(requestBodySpec.exchange(any())).thenAnswer(invocation -> expectedResponse);

        // Act
        FlaskAnalysisResponse response = flaskClient.runFactorRegression(testRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAlpha()).isEqualByComparingTo(new BigDecimal("0.000234"));
    }

    @Test
    @DisplayName("Should throw FlaskServiceException with BAD_GATEWAY on connection error")
    void runFactorRegression_connectionError_throwsBadGateway() {
        // Arrange
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/analysis/factor-regression")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(testRequest)).thenReturn(requestBodySpec);
        when(requestBodySpec.exchange(any())).thenThrow(new RestClientException("Connection refused"));

        // Act & Assert
        assertThatThrownBy(() -> flaskClient.runFactorRegression(testRequest))
                .isInstanceOf(FlaskServiceException.class)
                .satisfies(ex -> {
                    FlaskServiceException fse = (FlaskServiceException) ex;
                    assertThat(fse.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(fse.getMessage()).contains("unavailable");
                });
    }

    @Test
    @DisplayName("Should rethrow FlaskServiceException from exchange callback")
    void runFactorRegression_flaskError_rethrowsException() {
        // Arrange
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/analysis/factor-regression")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(testRequest)).thenReturn(requestBodySpec);
        when(requestBodySpec.exchange(any()))
                .thenThrow(new FlaskServiceException("Invalid ticker: XYZ", HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThatThrownBy(() -> flaskClient.runFactorRegression(testRequest))
                .isInstanceOf(FlaskServiceException.class)
                .satisfies(ex -> {
                    FlaskServiceException fse = (FlaskServiceException) ex;
                    assertThat(fse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(fse.getMessage()).contains("Invalid ticker");
                });
    }
}
