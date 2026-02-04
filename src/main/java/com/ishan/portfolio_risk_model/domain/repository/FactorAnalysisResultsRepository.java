package com.ishan.portfolio_risk_model.domain.repository;

import com.ishan.portfolio_risk_model.domain.entity.FactorAnalysisResultsEntity;
import com.ishan.portfolio_risk_model.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactorAnalysisResultsRepository extends JpaRepository<FactorAnalysisResultsEntity, Long> {

    List<FactorAnalysisResultsEntity> findByUserOrderByAnalysisDateDesc(UserEntity user);

    List<FactorAnalysisResultsEntity> findByUserIdOrderByAnalysisDateDesc(Long userId);
}
