package com.ipachi.pos.repo;

;
import com.ipachi.pos.model.CashMovement;
import com.ipachi.pos.model.TillSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findByTillSession(TillSession session);
            List<CashMovement> findByUserId(Long userId);
            Optional<CashMovement> findByIdAndUserId(Long id, Long userId);
}
