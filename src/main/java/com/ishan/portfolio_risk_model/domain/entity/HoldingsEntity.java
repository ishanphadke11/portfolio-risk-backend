package com.ishan.portfolio_risk_model.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}),
indexes = @Index(name = "idx_holdings_user", columnList = "user_id"))
public class HoldingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;
}
