package com.ipachi.pos.controller;

import com.ipachi.pos.dto.CheckoutRequest;
import com.ipachi.pos.dto.TillItem;
import com.ipachi.pos.model.InventoryItem;
import com.ipachi.pos.model.Transaction;
import com.ipachi.pos.service.DataStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/cash-till")
public class CashTillController {
    private final DataStore store;
    public CashTillController(DataStore store) { this.store = store; }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req) {
        double total = 0.0;
        if (req.getItems() != null) {
            for (TillItem it : req.getItems()) {
                total += it.getPrice() * it.getQty();
                Optional<InventoryItem> inv = store.findBySku(it.getSku());
                inv.ifPresent(item -> item.setQuantity(Math.max(0, item.getQuantity() - it.getQty())));
            }
        }
        String customerName = req.getCustomerName() != null ? req.getCustomerName() : "Walk-in";
        Transaction t = new Transaction(null, OffsetDateTime.now(), customerName, total);
        store.addTransaction(t);
        return ResponseEntity.ok(t);
    }
}
