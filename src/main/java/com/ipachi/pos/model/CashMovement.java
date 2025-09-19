package com.ipachi.pos.model;


import com.ipachi.pos.dto.CashMovementType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Entity
@Data
@Table(name = "cash_movement")
@SuperBuilder
public class CashMovement extends BaseOwnedEntity{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

            @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "till_session_id")
    private TillSession tillSession;

            @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashMovementType type;

            @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

            private String reference;
    private String reason;



            // getters and setters
        }
