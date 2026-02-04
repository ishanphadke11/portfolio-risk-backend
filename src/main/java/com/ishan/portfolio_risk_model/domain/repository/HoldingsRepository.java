package com.ishan.portfolio_risk_model.domain.repository;

import com.ishan.portfolio_risk_model.domain.entity.HoldingsEntity;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingsRepository extends JpaRepository<HoldingsEntity, Long> {

    List<HoldingsEntity> findByUser(UserEntity user);

    List<HoldingsEntity> findByUserId(Long userId);
    
    Optional<HoldingsEntity> findByUserAndTicker(UserEntity user, String ticker);

    boolean existsByUserAndTicker(UserEntity user, String ticker);
}
