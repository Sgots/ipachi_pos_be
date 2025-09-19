package com.ipachi.pos.controller;

import com.ipachi.pos.model.Terminal;
import com.ipachi.pos.service.TerminalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terminals")
public class TerminalController {
    private final TerminalService service;
    public TerminalController(TerminalService service) { this.service = service; }

    @GetMapping public List<Terminal> list() { return service.list(); }

    @PostMapping public Terminal create(@RequestBody Terminal t) { return service.create(t); }

    @PutMapping("/{id}") public Terminal update(@PathVariable Long id, @RequestBody Terminal t) {
        return service.update(id, t);
    }

    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { service.delete(id); }

    /** Idempotent; create if none. Call after user registration / first login. */
/*    @PostMapping("/default")
    public Terminal ensureDefault() { return service.ensureDefault(); }*/
}
