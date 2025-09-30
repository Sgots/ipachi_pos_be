package com.ipachi.pos.sim;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dev/seed")
public class SeedController {

    private final SeedService service;

    public SeedController(SeedService service) {
        this.service = service;
    }

    /**
     * Run the seeding on demand.
     * Example: POST /dev/seed/run?bizId=2&maxPerMonth=20
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(
            @RequestParam(name = "bizId", defaultValue = "2") Long bizId,
            @RequestParam(name = "maxPerMonth", defaultValue = "20") Integer maxPerMonth
    ) {
        return ResponseEntity.ok(service.run(bizId, maxPerMonth == null ? 20 : maxPerMonth));
    }
}
