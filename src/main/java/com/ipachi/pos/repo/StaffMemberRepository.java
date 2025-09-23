// src/main/java/com/ipachi/pos/repo/StaffMemberRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;


import java.util.List;
import java.util.Optional;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {
    List<StaffMember> findByBusinessIdOrderByIdAsc(Long businessId);
    Optional<StaffMember> findByIdAndBusinessId(Long id, Long businessId);
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<StaffMember> findByUserIdAndBusinessId(Long userId, Long businessId);

}
