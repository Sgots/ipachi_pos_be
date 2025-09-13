package com.ipachi.pos.controller;

import com.ipachi.pos.model.Customer;
import com.ipachi.pos.service.DataStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final DataStore store;

    public CustomerController(DataStore store) { this.store = store; }

    @GetMapping
    public ResponseEntity<List<Customer>> list(@RequestParam(value = "search", required = false) String q) {
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(store.searchCustomers(q));
        }
        return ResponseEntity.ok(store.listCustomers());
    }

    @PostMapping
    public ResponseEntity<Customer> create(@Valid @RequestBody Customer c) {
        return ResponseEntity.ok(store.addCustomer(c));
    }
}
