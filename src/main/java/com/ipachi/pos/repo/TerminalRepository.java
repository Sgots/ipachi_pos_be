package com.ipachi.pos.repository;

import com.ipachi.pos.model.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {
    List<Terminal> findByUserId(Long userId);
    Optional<Terminal> findByIdAndUserId(Long id, Long userId);
    Optional<Terminal> findFirstByUserIdOrderByIdAsc(Long userId);
    boolean existsByUserId(Long userId);
}
