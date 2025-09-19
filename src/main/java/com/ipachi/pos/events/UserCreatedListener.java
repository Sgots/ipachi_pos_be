package com.ipachi.pos.events;

import com.ipachi.pos.service.TerminalService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedListener {

    private final TerminalService terminals;

    public UserCreatedListener(TerminalService terminals) { this.terminals = terminals; }

    @EventListener
    public void onUserCreated(UserCreatedEvent evt) {
        // Idempotent: will create "Default Terminal" only if none exists
        terminals.ensureDefaultForUser(evt.userId());
    }
}
