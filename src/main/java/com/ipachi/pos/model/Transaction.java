package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tx_head")
@SuperBuilder
public class Transaction extends BaseOwnedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private String customerName = "Walk-in";

    @Column(nullable = true, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    // add near other fields
    @Column(name = "terminal_id")
    private Long terminalId;

    public Long getTerminalId() { return terminalId; }
    public void setTerminalId(Long terminalId) { this.terminalId = terminalId; }

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionLine> lines = new ArrayList<>();

    // getters/setters
    public Long getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<TransactionLine> getLines() { return lines; }
    public void setLines(List<TransactionLine> lines) { this.lines = lines; }
}
