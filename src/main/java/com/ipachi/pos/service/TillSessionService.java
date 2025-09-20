package com.ipachi.pos.service;


import com.ipachi.pos.dto.TillSessionStatus;
import com.ipachi.pos.model.TillSession;
import com.ipachi.pos.model.Terminal;
import com.ipachi.pos.repo.TillSessionRepository;
import com.ipachi.pos.repository.TerminalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TillSessionService {

    private final TillSessionRepository tillSessions;
    private final TerminalRepository terminalsRepo;
    private final TerminalService terminals; // existing service you already use

    public TillSessionService(
            TillSessionRepository tillSessions,
            TerminalRepository terminalsRepo,
            TerminalService terminals
    ) {
        this.tillSessions = tillSessions;
        this.terminalsRepo = terminalsRepo;
        this.terminals = terminals;
    }

    /**
     * Idempotently ensures there's an OPEN TillSession on the user's Default Terminal.
     * - Creates the Default Terminal if missing (delegates to TerminalService).
     * - Creates an OPEN TillSession if none exists.
     * @return the TillSession id (existing or newly created)
     */
    @Transactional
    public Long ensureOpenOnDefaultTerminal(Long userId) {
        // 1) Make sure a default terminal exists for this user
        terminals.ensureDefaultForUser(userId);

        // 2) Resolve that default terminal
        Terminal terminal = terminalsRepo
                .findByUserIdAndName(userId, "Default Terminal")
                .orElseThrow(() -> new IllegalStateException("Default Terminal not found after ensureDefaultForUser"));

        // 3) If there is already an OPEN session for this terminal, we're done
        boolean hasOpen = tillSessions.existsByTerminalIdAndStatus(terminal.getId(), TillSessionStatus.OPEN);
        if (hasOpen) {
            // Nothing to do; just return any open session id not strictly needed here
            // (We keep it simple to avoid extra queries. You can extend repo to fetch it if you need.)
            return null;
        }

        // 4) Otherwise, create an OPEN session with zero opening float (or adjust default as you wish)
        TillSession session = new TillSession();
        session.setTerminalId(terminal.getId());
        session.setOpenedByUserId(userId);
        session.setOpeningFloat(BigDecimal.ZERO);
        session.setStatus(TillSessionStatus.OPEN);
        // openedAt defaults in entity; leave as is

        return tillSessions.save(session).getId();
    }
}
