package com.ipachi.pos.repo;

;
import com.ipachi.pos.model.CashMovement;
import com.ipachi.pos.model.TillSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

        public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findByTillSession(TillSession session);
}
