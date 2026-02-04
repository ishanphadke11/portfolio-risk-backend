package com.ishan.portfolio_risk_model.domain.repository;

import com.ishan.portfolio_risk_model.domain.entity.HoldingsEntity;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HoldingsRepositoryTest {

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        holdingsRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new UserEntity();
        testUser.setEmail("investor@example.com");
        testUser.setPasswordHash("hash");
        testUser.setRole(UserEntity.Role.USER);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should save and retrieve holdings by user")
    void findByUser_returnsUserHoldings() {
        HoldingsEntity holding1 = createHolding("AAPL", "10.5");
        HoldingsEntity holding2 = createHolding("GOOGL", "5.0");
        holdingsRepository.save(holding1);
        holdingsRepository.save(holding2);

        List<HoldingsEntity> holdings = holdingsRepository.findByUser(testUser);

        assertThat(holdings).hasSize(2);
        assertThat(holdings).extracting(HoldingsEntity::getTicker)
                .containsExactlyInAnyOrder("AAPL", "GOOGL");
    }

    @Test
    @DisplayName("Should find holding by user and ticker")
    void findByUserAndTicker_existingHolding_returnsHolding() {
        holdingsRepository.save(createHolding("TSLA", "25.0"));

        var found = holdingsRepository.findByUserAndTicker(testUser, "TSLA");

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualByComparingTo(new BigDecimal("25.0"));
    }

    @Test
    @DisplayName("Should check if user already has ticker")
    void existsByUserAndTicker_duplicateTicker_returnsTrue() {
        holdingsRepository.save(createHolding("AAPL", "10.0"));

        assertThat(holdingsRepository.existsByUserAndTicker(testUser, "AAPL")).isTrue();
        assertThat(holdingsRepository.existsByUserAndTicker(testUser, "MSFT")).isFalse();
    }

    private HoldingsEntity createHolding(String ticker, String quantity) {
        HoldingsEntity holding = new HoldingsEntity();
        holding.setUser(testUser);
        holding.setTicker(ticker);
        holding.setQuantity(new BigDecimal(quantity));
        return holding;
    }
}
