package com.ishan.portfolio_risk_model.service;

import com.ishan.portfolio_risk_model.domain.entity.FactorAnalysisResultsEntity;
import com.ishan.portfolio_risk_model.domain.entity.HoldingsEntity;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import com.ishan.portfolio_risk_model.domain.repository.FactorAnalysisResultsRepository;
import com.ishan.portfolio_risk_model.domain.repository.HoldingsRepository;
import com.ishan.portfolio_risk_model.dto.AnalysisResponse;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisRequest;
import com.ishan.portfolio_risk_model.dto.FlaskAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private HoldingsRepository holdingsRepository;

    @Mock
    private FactorAnalysisResultsRepository resultsRepository;

    @Mock
    private FlaskClient flaskClient;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AnalysisService analysisService;

    private UserEntity testUser;
    private HoldingsEntity testHolding;
    private FlaskAnalysisResponse flaskResponse;
    private FactorAnalysisResultsEntity savedEntity;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash");
        testUser.setRole(UserEntity.Role.USER);

        // Create test holding
        testHolding = new HoldingsEntity();
        testHolding.setId(1L);
        testHolding.setUser(testUser);
        testHolding.setTicker("AAPL");
        testHolding.setQuantity(new BigDecimal("10"));

        // Create Flask response
        flaskResponse = new FlaskAnalysisResponse();
        flaskResponse.setAlpha(new BigDecimal("0.000234"));
        flaskResponse.setBetaMkt(new BigDecimal("0.856420"));
        flaskResponse.setBetaSmb(new BigDecimal("0.123456"));
        flaskResponse.setBetaHml(new BigDecimal("-0.045678"));
        flaskResponse.setBetaRmw(new BigDecimal("0.032145"));
        flaskResponse.setBetaCma(new BigDecimal("-0.012345"));
        flaskResponse.setRSquared(new BigDecimal("0.876543"));
        flaskResponse.setTStats(Map.of("alpha", new BigDecimal("1.2345"), "mkt", new BigDecimal("15.6789")));

        // Create saved entity (what the repository returns after save)
        savedEntity = new FactorAnalysisResultsEntity();
        savedEntity.setId(1L);
        savedEntity.setUser(testUser);
        savedEntity.setAnalysisDate(LocalDateTime.now());
        savedEntity.setAlpha(flaskResponse.getAlpha());
        savedEntity.setBetaMkt(flaskResponse.getBetaMkt());
        savedEntity.setBetaSmb(flaskResponse.getBetaSmb());
        savedEntity.setBetaHml(flaskResponse.getBetaHml());
        savedEntity.setBetaRmw(flaskResponse.getBetaRmw());
        savedEntity.setBetaCma(flaskResponse.getBetaCma());
        savedEntity.setRSquared(flaskResponse.getRSquared());

        // Mock security context
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should run analysis and return result with tStats")
    void runAnalysis_withHoldings_returnsResult() {
        // Arrange
        when(holdingsRepository.findByUser(testUser)).thenReturn(List.of(testHolding));
        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class))).thenReturn(flaskResponse);
        when(resultsRepository.save(any(FactorAnalysisResultsEntity.class))).thenReturn(savedEntity);

        // Act
        AnalysisResponse response = analysisService.runAnalysis(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAlpha()).isEqualByComparingTo(new BigDecimal("0.000234"));
        assertThat(response.getBetaMkt()).isEqualByComparingTo(new BigDecimal("0.856420"));
        assertThat(response.getRSquared()).isEqualByComparingTo(new BigDecimal("0.876543"));
        assertThat(response.getTStats()).isNotNull();
        assertThat(response.getTStats()).containsKey("mkt");

        verify(resultsRepository).save(any(FactorAnalysisResultsEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when user has no holdings")
    void runAnalysis_noHoldings_throwsException() {
        // Arrange
        when(holdingsRepository.findByUser(testUser)).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> analysisService.runAnalysis(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No holdings found");

        verify(flaskClient, never()).runFactorRegression(any());
    }

    @Test
    @DisplayName("Should use provided dates in Flask request")
    void runAnalysis_withDates_usesProvidedDates() {
        // Arrange
        when(holdingsRepository.findByUser(testUser)).thenReturn(List.of(testHolding));
        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class))).thenReturn(flaskResponse);
        when(resultsRepository.save(any(FactorAnalysisResultsEntity.class))).thenReturn(savedEntity);

        // Act
        analysisService.runAnalysis(LocalDate.of(2020, 6, 15), LocalDate.of(2024, 6, 15));

        // Assert
        ArgumentCaptor<FlaskAnalysisRequest> captor = ArgumentCaptor.forClass(FlaskAnalysisRequest.class);
        verify(flaskClient).runFactorRegression(captor.capture());
        assertThat(captor.getValue().getStartDate()).isEqualTo("2020-06-15");
        assertThat(captor.getValue().getEndDate()).isEqualTo("2024-06-15");
    }

    @Test
    @DisplayName("Should use default dates when not provided")
    void runAnalysis_withoutDates_usesDefaults() {
        // Arrange
        when(holdingsRepository.findByUser(testUser)).thenReturn(List.of(testHolding));
        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class))).thenReturn(flaskResponse);
        when(resultsRepository.save(any(FactorAnalysisResultsEntity.class))).thenReturn(savedEntity);

        // Act
        analysisService.runAnalysis(null, null);

        // Assert
        ArgumentCaptor<FlaskAnalysisRequest> captor = ArgumentCaptor.forClass(FlaskAnalysisRequest.class);
        verify(flaskClient).runFactorRegression(captor.capture());
        assertThat(captor.getValue().getEndDate()).isEqualTo(LocalDate.now().toString());
        assertThat(captor.getValue().getStartDate()).isEqualTo(LocalDate.now().minusYears(3).toString());
    }

    @Test
    @DisplayName("Should propagate FlaskServiceException")
    void runAnalysis_flaskError_throwsFlaskServiceException() {
        // Arrange
        when(holdingsRepository.findByUser(testUser)).thenReturn(List.of(testHolding));
        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class)))
                .thenThrow(new FlaskServiceException("Invalid ticker: XYZ", HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThatThrownBy(() -> analysisService.runAnalysis(null, null))
                .isInstanceOf(FlaskServiceException.class)
                .hasMessageContaining("Invalid ticker");

        verify(resultsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should build Flask request with correct holdings")
    void runAnalysis_multipleHoldings_buildsCorrectRequest() {
        // Arrange
        HoldingsEntity holding2 = new HoldingsEntity();
        holding2.setId(2L);
        holding2.setUser(testUser);
        holding2.setTicker("TSLA");
        holding2.setQuantity(new BigDecimal("5"));

        when(holdingsRepository.findByUser(testUser)).thenReturn(List.of(testHolding, holding2));
        when(flaskClient.runFactorRegression(any(FlaskAnalysisRequest.class))).thenReturn(flaskResponse);
        when(resultsRepository.save(any(FactorAnalysisResultsEntity.class))).thenReturn(savedEntity);

        // Act
        analysisService.runAnalysis(null, null);

        // Assert
        ArgumentCaptor<FlaskAnalysisRequest> captor = ArgumentCaptor.forClass(FlaskAnalysisRequest.class);
        verify(flaskClient).runFactorRegression(captor.capture());
        assertThat(captor.getValue().getHoldings()).hasSize(2);
        assertThat(captor.getValue().getHoldings().get(0).getTicker()).isEqualTo("AAPL");
        assertThat(captor.getValue().getHoldings().get(0).getQuantity()).isEqualTo(10);
        assertThat(captor.getValue().getHoldings().get(1).getTicker()).isEqualTo("TSLA");
        assertThat(captor.getValue().getHoldings().get(1).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should return history for current user")
    void getHistory_returnsResults() {
        // Arrange
        when(resultsRepository.findByUserOrderByAnalysisDateDesc(testUser))
                .thenReturn(List.of(savedEntity));

        // Act
        List<AnalysisResponse> history = analysisService.getHistory();

        // Assert
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getId()).isEqualTo(1L);
        assertThat(history.get(0).getTStats()).isNull();
    }

    @Test
    @DisplayName("Should return analysis result by ID")
    void getAnalysisById_found_returnsResult() {
        // Arrange
        when(resultsRepository.findById(1L)).thenReturn(Optional.of(savedEntity));

        // Act
        AnalysisResponse response = analysisService.getAnalysisById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAlpha()).isEqualByComparingTo(new BigDecimal("0.000234"));
        assertThat(response.getTStats()).isNull();
    }

    @Test
    @DisplayName("Should throw exception for non-existent analysis ID")
    void getAnalysisById_notFound_throwsException() {
        // Arrange
        when(resultsRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> analysisService.getAnalysisById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Analysis result not found");
    }

    @Test
    @DisplayName("Should throw exception when accessing another user's analysis")
    void getAnalysisById_differentUser_throwsException() {
        // Arrange
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);

        FactorAnalysisResultsEntity otherResult = new FactorAnalysisResultsEntity();
        otherResult.setId(1L);
        otherResult.setUser(otherUser);

        when(resultsRepository.findById(1L)).thenReturn(Optional.of(otherResult));

        // Act & Assert
        assertThatThrownBy(() -> analysisService.getAnalysisById(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Analysis result not found");
    }
}
