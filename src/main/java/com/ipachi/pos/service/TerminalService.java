package com.ipachi.pos.service;

import com.ipachi.pos.model.Terminal;
import com.ipachi.pos.repository.TerminalRepository;
import com.ipachi.pos.security.CurrentRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TerminalService {
    private final TerminalRepository repo;
    private final CurrentRequest ctx;

    public TerminalService(TerminalRepository repo, CurrentRequest ctx) {
        this.repo = repo; this.ctx = ctx;
    }

    public List<Terminal> list() {
        return repo.findByUserId(ctx.getUserId());
    }

    public Terminal create(Terminal t) {
        t.setUserId(ctx.getUserId());
        t.setCreatedAt(OffsetDateTime.now());
        t.setUpdatedAt(OffsetDateTime.now());
        return repo.save(t);
    }

    public Terminal update(Long id, Terminal patch) {
        Terminal existing = repo.findByIdAndUserId(id, ctx.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getCode() != null) existing.setCode(patch.getCode());
        existing.setActive(patch.isActive());
        existing.setLocation(patch.getLocation());
        existing.setUpdatedAt(OffsetDateTime.now());
        return repo.save(existing);
    }

    public void delete(Long id) {
        Terminal existing = repo.findByIdAndUserId(id, ctx.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));
        repo.delete(existing);
    }

    /** Idempotent: ensures at least one terminal exists; creates "Default" if none. */
    /** Idempotent: ensures at least one terminal exists; creates "Default" if none. */
    public Terminal ensureDefaultForUser(Long userId) {
        return repo.findFirstByUserIdOrderByIdAsc(userId).orElseGet(() -> {
            // Use @SuperBuilder with all fields including inherited ones
            Terminal t = Terminal.builder()
                    .userId(userId) // Inherited from BaseOwnedEntity
                    .name("Default Terminal")
                    .code("TERM-1")
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            return repo.save(t);
        });
    }
    /** Validates that ctx.terminalId (if present) belongs to ctx.userId. */

}
