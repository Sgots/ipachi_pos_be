package com.ipachi.pos.controller;

import com.ipachi.pos.model.FeatureFlags;
import com.ipachi.pos.service.DataStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/features")
public class FeatureController {
    private final DataStore store;
    public FeatureController(DataStore store) { this.store = store; }

    @GetMapping("/global")
    public ResponseEntity<FeatureFlags> getGlobal() {
        return ResponseEntity.ok(store.getGlobalFlags());
    }

    @PutMapping("/global")
    public ResponseEntity<FeatureFlags> updateGlobal(@RequestBody FeatureFlags flags) {
        store.setGlobalFlags(flags);
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<FeatureFlags> getCustomer(@PathVariable("id") long id) {
        return ResponseEntity.ok(store.getCustomerFlags(id));
    }

    @PutMapping("/customer/{id}")
    public ResponseEntity<FeatureFlags> updateCustomer(@PathVariable("id") long id, @RequestBody FeatureFlags flags) {
        store.setCustomerFlags(id, flags);
        return ResponseEntity.ok(flags);
    }
}
