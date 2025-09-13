package com.ipachi.pos.controller;

import com.ipachi.pos.model.Transaction;
import com.ipachi.pos.service.DataStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final DataStore store;
    public TransactionController(DataStore store) { this.store = store; }

    @GetMapping
    public ResponseEntity<List<Transaction>> list() {
        return ResponseEntity.ok(store.listTransactions());
    }
}
