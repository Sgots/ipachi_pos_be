package com.ipachi.pos.service;



import com.ipachi.pos.dto.*;
import com.ipachi.pos.model.CashMovement;
import com.ipachi.pos.model.TillSession;
import com.ipachi.pos.repo.CashMovementRepository;
import com.ipachi.pos.repo.TillSessionRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

        import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

        @Service
public class TillService {
    private final TillSessionRepository tillRepo;
    private final CashMovementRepository movRepo;
            private final CurrentRequest ctx; // Inject CurrentRequest to get userId from headers
    public TillService(TillSessionRepository tillRepo, CashMovementRepository movRepo, CurrentRequest ctx) {
                this.tillRepo = tillRepo; this.movRepo = movRepo; this.ctx = ctx;
            }

            @Transactional
    public TillSession open(OpenTillRequest req) {
                tillRepo.findFirstByTerminalIdAndStatus(req.terminalId, TillSessionStatus.OPEN)
                                .ifPresent(s -> { throw new IllegalStateException("Active till already open for terminal"); });
                TillSession s = new TillSession();
                s.setTerminalId(req.terminalId);
                s.setOpenedByUserId(req.openedByUserId);
                s.setOpeningFloat(req.openingFloat);
                s.setNotes(req.notes);
                return tillRepo.save(s);
            }

            public TillSession getActive(Long terminalId) {
                return tillRepo.findFirstByTerminalIdAndStatus(terminalId, TillSessionStatus.OPEN)
                                .orElse(null);
            }

            @Transactional
            public CashMovement addMovement(Long tillId, CashMovementType type, MovementRequest req) {
                TillSession s = tillRepo.findById(tillId).orElseThrow();
                if (s.getStatus() == TillSessionStatus.CLOSED) {
                    throw new IllegalStateException("Till is closed");
                }

                // Use builder pattern with userId if it extends BaseOwnedEntity
                CashMovement m = CashMovement.builder()
                        .tillSession(s)
                        .type(type)
                        .amount(req.amount)
                        .reference(req.reference)
                        .reason(req.reason)
                        .userId(ctx.getUserId()) // Add this if it extends BaseOwnedEntity TO CHANGE
                        .createdAt(OffsetDateTime.now()) // Add if needed
                        .updatedAt(OffsetDateTime.now()) // Add if needed
                        .build();

                return movRepo.save(m);
            }
            public TillSummary summary(Long tillId) {
                TillSession s = tillRepo.findById(tillId).orElseThrow();
                List<CashMovement> list = movRepo.findByTillSession(s);
                BigDecimal sales   = sum(list, CashMovementType.SALE);
                BigDecimal refunds = sum(list, CashMovementType.REFUND);
                BigDecimal cashIn  = sum(list, CashMovementType.CASH_IN);
                BigDecimal cashOut = sum(list, CashMovementType.CASH_OUT);
                BigDecimal payouts = sum(list, CashMovementType.PAYOUT);
                BigDecimal expected = safe(s.getOpeningFloat())
                                .add(sales).subtract(refunds).add(cashIn).subtract(cashOut).subtract(payouts);
                TillSummary res = new TillSummary();
                res.openingFloat = s.getOpeningFloat();
                res.sales = sales; res.refunds = refunds; res.cashIn = cashIn; res.cashOut = cashOut; res.payouts = payouts;
                res.expectedCash = expected;
                res.closingCashActual = s.getClosingCashActual();
                res.overShort = s.getOverShort();
                return res;
            }

            @Transactional
    public TillSession close(Long tillId, CloseTillRequest req) {
                TillSession s = tillRepo.findById(tillId).orElseThrow();
                if (s.getStatus() == TillSessionStatus.CLOSED) return s;
                TillSummary sm = summary(tillId);
                s.setExpectedCash(sm.expectedCash);
                s.setClosingCashActual(req.closingCashActual);
                s.setOverShort(req.closingCashActual.subtract(sm.expectedCash));
                s.setNotes(req.notes);
                s.setStatus(TillSessionStatus.CLOSED);
                s.setClosedAt(java.time.OffsetDateTime.now());
                return tillRepo.save(s);
            }

            private BigDecimal sum(List<CashMovement> list, CashMovementType t) {
                return list.stream().filter(m -> m.getType()==t)
                                .map(CashMovement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
    private BigDecimal safe(BigDecimal v) { return v==null? BigDecimal.ZERO: v; }
}
