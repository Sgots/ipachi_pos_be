package com.ipachi.pos.events;

import com.ipachi.pos.service.TerminalService;
import com.ipachi.pos.service.TillSessionService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedListener {

    private final TerminalService terminals;
    private final TillSessionService tillSessions;

    public UserCreatedListener(TerminalService terminals, TillSessionService tillSessions) {
        this.terminals = terminals;
        this.tillSessions = tillSessions;
    }

    @EventListener
    public void onUserCreated(UserCreatedEvent evt) {
        // Idempotent: will create "Default Terminal" only if none exists
        terminals.ensureDefaultForUser(evt.userId());

        // Idempotent: will open a default OPEN till session if none exists
        tillSessions.ensureOpenOnDefaultTerminal(evt.userId());
    }
}
