package com.ishan.portfolio_risk_model.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "factor_analysis_results",
        indexes = @Index(name = "idx_results_user", columnList = "user_id"))
public class FactorAnalysisResultsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;

    @Column(precision = 19, scale = 10, nullable = false)
    private BigDecimal alpha;

    @Column(name = "beta_mkt", precision = 19, scale = 10, nullable = false)
    private BigDecimal betaMkt;

    @Column(name = "beta_smb", precision = 19, scale = 10, nullable = false)
    private BigDecimal betaSmb;

    @Column(name = "beta_hml", precision = 19, scale = 10, nullable = false)
    private BigDecimal betaHml;

    @Column(name = "beta_rmw", precision = 19, scale = 10, nullable = false)
    private BigDecimal betaRmw;

    @Column(name = "beta_cma", precision = 19, scale = 10, nullable = false)
    private BigDecimal betaCma;

    @Column(name = "r_squared", precision = 10, scale = 6, nullable = false)
    private BigDecimal rSquared;
}
