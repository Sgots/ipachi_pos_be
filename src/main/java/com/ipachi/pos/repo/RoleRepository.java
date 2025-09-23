// src/main/java/com/ipachi/pos/repo/RoleRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByBusinessIdAndNameIgnoreCase(Long businessId, String name);
    boolean existsByBusinessIdAndNameIgnoreCaseAndIdNot(Long businessId, String name, Long id);
    List<Role> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Role> findByIdAndBusinessId(Long id, Long businessId);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermsByIdAndBusinessId(Long id, Long businessId);
}
