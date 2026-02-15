package com.ishan.portfolio_risk_model.service;

import com.ishan.portfolio_risk_model.domain.entity.HoldingsEntity;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import com.ishan.portfolio_risk_model.domain.repository.HoldingsRepository;
import com.ishan.portfolio_risk_model.dto.HoldingsRequest;
import com.ishan.portfolio_risk_model.dto.HoldingsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HoldingService.
 */
@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private HoldingsRepository holdingsRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private HoldingService holdingService;

    private UserEntity testUser;
    private HoldingsEntity testHolding;
    private HoldingsRequest holdingRequest;

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
        testHolding.setQuantity(new BigDecimal("10.5"));

        // Create request
        holdingRequest = new HoldingsRequest("AAPL", new BigDecimal("10.5"));

        // Mock security context
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should get all holdings for current user")
    void getHoldings_returnsUserHoldings() {
        // Arrange
        HoldingsEntity holding2 = new HoldingsEntity();
        holding2.setId(2L);
        holding2.setUser(testUser);
        holding2.setTicker("GOOGL");
        holding2.setQuantity(new BigDecimal("5.0"));

        when(holdingsRepository.findByUser(testUser)).thenReturn(List.of(testHolding, holding2));

        // Act
        List<HoldingsResponse> holdings = holdingService.getHoldings();

        // Assert
        assertThat(holdings).hasSize(2);
        assertThat(holdings.get(0).getTicker()).isEqualTo("AAPL");
        assertThat(holdings.get(1).getTicker()).isEqualTo("GOOGL");
    }

    @Test
    @DisplayName("Should create new holding successfully")
    void createHolding_newTicker_returnsHolding() {
        // Arrange
        when(holdingsRepository.existsByUserAndTicker(testUser, "AAPL")).thenReturn(false);
        when(holdingsRepository.save(any(HoldingsEntity.class))).thenReturn(testHolding);

        // Act
        HoldingsResponse response = holdingService.createHolding(holdingRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTicker()).isEqualTo("AAPL");
        assertThat(response.getQuantity()).isEqualByComparingTo(new BigDecimal("10.5"));

        // Verify holding was saved
        ArgumentCaptor<HoldingsEntity> captor = ArgumentCaptor.forClass(HoldingsEntity.class);
        verify(holdingsRepository).save(captor.capture());
        assertThat(captor.getValue().getTicker()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("Should throw exception when ticker already exists")
    void createHolding_duplicateTicker_throwsException() {
        // Arrange
        when(holdingsRepository.existsByUserAndTicker(testUser, "AAPL")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> holdingService.createHolding(holdingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already have a holding");

        verify(holdingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update holding quantity")
    void updateHolding_validId_updatesQuantity() {
        // Arrange
        HoldingsRequest updateRequest = new HoldingsRequest("AAPL", new BigDecimal("25.0"));
        when(holdingsRepository.findById(1L)).thenReturn(Optional.of(testHolding));
        when(holdingsRepository.save(any(HoldingsEntity.class))).thenReturn(testHolding);

        // Act
        HoldingsResponse response = holdingService.updateHolding(1L, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        verify(holdingsRepository).save(testHolding);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent holding")
    void updateHolding_notFound_throwsException() {
        // Arrange
        when(holdingsRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> holdingService.updateHolding(99L, holdingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Holding not found");
    }

    @Test
    @DisplayName("Should throw exception when updating another user's holding")
    void updateHolding_differentUser_throwsException() {
        // Arrange
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);

        HoldingsEntity otherHolding = new HoldingsEntity();
        otherHolding.setId(1L);
        otherHolding.setUser(otherUser);

        when(holdingsRepository.findById(1L)).thenReturn(Optional.of(otherHolding));

        // Act & Assert
        assertThatThrownBy(() -> holdingService.updateHolding(1L, holdingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Holding not found");
    }

    @Test
    @DisplayName("Should delete holding successfully")
    void deleteHolding_validId_deletesHolding() {
        // Arrange
        when(holdingsRepository.findById(1L)).thenReturn(Optional.of(testHolding));

        // Act
        holdingService.deleteHolding(1L);

        // Assert
        verify(holdingsRepository).delete(testHolding);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent holding")
    void deleteHolding_notFound_throwsException() {
        // Arrange
        when(holdingsRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> holdingService.deleteHolding(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Holding not found");
    }

    @Test
    @DisplayName("Should throw exception when deleting another user's holding")
    void deleteHolding_differentUser_throwsException() {
        // Arrange
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);

        HoldingsEntity otherHolding = new HoldingsEntity();
        otherHolding.setId(1L);
        otherHolding.setUser(otherUser);

        when(holdingsRepository.findById(1L)).thenReturn(Optional.of(otherHolding));

        // Act & Assert
        assertThatThrownBy(() -> holdingService.deleteHolding(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Holding not found");

        verify(holdingsRepository, never()).delete(any());
    }
}
