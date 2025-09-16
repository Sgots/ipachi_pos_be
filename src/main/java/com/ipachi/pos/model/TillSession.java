package com.ipachi.pos.model;


import com.ipachi.pos.dto.TillSessionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "till_session")
public class TillSession {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

     @Column(nullable = false)
 private String terminalId;
  @Column(nullable = false)
  private Long openedByUserId;
   @Column(nullable = false)
 private OffsetDateTime openedAt = OffsetDateTime.now();
    @Column(nullable = false, precision = 18, scale = 2)
   private BigDecimal openingFloat;

           @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TillSessionStatus status = TillSessionStatus.OPEN;
           private OffsetDateTime closedAt;

           @Column(precision = 18, scale = 2)
   private BigDecimal closingCashActual;

            @Column(precision = 18, scale = 2)
 private BigDecimal expectedCash;

           @Column(precision = 18, scale = 2)
  private BigDecimal overShort;

            @Column(length = 500)
 private String notes;

       // getters and setters omitted for brevity
           // ... generate all getters/setters
       }
