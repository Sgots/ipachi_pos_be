package com.ipachi.pos.controller;


import com.ipachi.pos.dto.*;
import com.ipachi.pos.model.CashMovement;
import com.ipachi.pos.model.TillSession;
import com.ipachi.pos.service.TillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        @RestController
@RequestMapping("/api/tills")
public class TillController {
    private final TillService service;
    public TillController(TillService service) { this.service = service; }

            @PostMapping("/open")
    public ResponseEntity<ApiResponse<TillSession>> open(@RequestBody OpenTillRequest req) {
                try {
                        return ResponseEntity.ok(ApiResponse.ok(service.open(req)));
                    } catch (IllegalStateException ex) {
                        return ResponseEntity.badRequest().body(ApiResponse.err(1001, ex.getMessage()));
                    }
            }

            @GetMapping("/active")
    public ResponseEntity<ApiResponse<TillSession>> active(@RequestParam String terminalId) {
                TillSession s = service.getActive(terminalId);
                return ResponseEntity.ok(ApiResponse.ok(s));
            }

            @PostMapping("/{id}/cash-in")
    public ResponseEntity<ApiResponse<CashMovement>> cashIn(@PathVariable Long id, @RequestBody MovementRequest req) {
                return ResponseEntity.ok(ApiResponse.ok(service.addMovement(id, CashMovementType.CASH_IN, req)));
            }
    @PostMapping("/{id}/cash-out")
    public ResponseEntity<ApiResponse<CashMovement>> cashOut(@PathVariable Long id, @RequestBody MovementRequest req) {
                return ResponseEntity.ok(ApiResponse.ok(service.addMovement(id, CashMovementType.CASH_OUT, req)));
            }
    @PostMapping("/{id}/record-sale")
    public ResponseEntity<ApiResponse<CashMovement>> sale(@PathVariable Long id, @RequestBody MovementRequest req) {
                return ResponseEntity.ok(ApiResponse.ok(service.addMovement(id, CashMovementType.SALE, req)));
            }
    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<CashMovement>> refund(@PathVariable Long id, @RequestBody MovementRequest req) {
                return ResponseEntity.ok(ApiResponse.ok(service.addMovement(id, CashMovementType.REFUND, req)));
            }

            @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<TillSummary>> summary(@PathVariable Long id) {
                return ResponseEntity.ok(ApiResponse.ok(service.summary(id)));
            }

            @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<TillSession>> close(@PathVariable Long id, @RequestBody CloseTillRequest req) {
                return ResponseEntity.ok(ApiResponse.ok(service.close(id, req)));
            }
}
