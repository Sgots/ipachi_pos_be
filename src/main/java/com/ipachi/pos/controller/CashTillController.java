package com.ipachi.pos.controller;


import com.ipachi.pos.dto.CashMovementType;
import com.ipachi.pos.dto.CheckoutRequest;
import com.ipachi.pos.dto.CheckoutResponse;
import com.ipachi.pos.dto.MovementRequest;
import com.ipachi.pos.model.TillSession;
import com.ipachi.pos.model.Transaction;
import com.ipachi.pos.service.CheckoutService;
import com.ipachi.pos.service.TillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cash-till")
public class CashTillController {

    private final CheckoutService checkoutService;
    private final TillService tillService;

    public CashTillController(CheckoutService checkoutService, TillService tillService) {
        this.checkoutService = checkoutService;
        this.tillService = tillService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest req) {
        // 1) Persist transaction + lines, update inventory
        Transaction tx = checkoutService.checkout(req);

        // 2) If a till is open for the terminal, record SALE
        if (req.getTerminalId() != null) {
            TillSession active = tillService.getActive(req.getTerminalId());
            if (active != null) {
                MovementRequest mr = new MovementRequest();
                mr.amount = tx.getTotal();
                mr.reference = "TX-" + tx.getId();
                mr.reason = "Checkout";
                tillService.addMovement(active.getId(), CashMovementType.SALE, mr);
            }
        }

        CheckoutResponse resp = new CheckoutResponse(
                tx.getId(),
                tx.getCreatedAt(),
                tx.getTotal(),
                tx.getCustomerName(),
                "Checkout saved"
        );
        return ResponseEntity.ok(resp);
    }
}
