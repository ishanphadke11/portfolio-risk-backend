package com.ishan.portfolio_risk_model.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fama_french_factors")
public class FamaFrenchFactorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "factors_id_seq")
    private Long id;

    @Column(name = "factor_date", nullable = false)
    private LocalDate factorDate;

    @Column(precision = 19, scale = 10, nullable = false)
    private BigDecimal mktRf;

    @Column(precision = 19, scale = 10, nullable = false)
    private BigDecimal smb;

    @Column(precision = 19, scale = 10, nullable = false)
    private BigDecimal hml;

    @Column(precision = 19, scale = 10, nullable = false)
    private BigDecimal rmw;

    @Column(precision = 19, scale = 10, nullable = false)
    private BigDecimal cma;

    @Column(precision = 19, scale = 10, nullable = false)
    private BigDecimal rf;
}
