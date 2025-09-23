// src/main/java/com/ipachi/pos/repo/RolePermissionRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
}
