package com.ishan.portfolio_risk_model.domain.repository;

import com.ishan.portfolio_risk_model.domain.entity.FamaFrenchFactorEntity;
import org.hibernate.boot.models.JpaAnnotations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamaFrenchFactorRepository extends JpaRepository<FamaFrenchFactorEntity, Long> {

    Optional<FamaFrenchFactorEntity> findByFactorDate(LocalDate factorDate);

    List<FamaFrenchFactorEntity> findByFactorDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByFactorDate(LocalDate factorDate);
}
