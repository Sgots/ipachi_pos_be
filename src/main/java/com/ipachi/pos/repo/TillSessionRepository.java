package com.ipachi.pos.repo;


import com.ipachi.pos.dto.TillSessionStatus;
import com.ipachi.pos.model.TillSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

        public interface TillSessionRepository extends JpaRepository<TillSession, Long> {
            boolean existsByTerminalIdAndStatus(Long terminalId, TillSessionStatus status);

            Optional<TillSession> findFirstByTerminalIdAndStatus(Long terminalId, TillSessionStatus status);
}
