// src/main/java/com/ipachi/pos/service/RoleService.java
package com.ipachi.pos.service;


import com.ipachi.pos.dto.CreateRoleRequest;
import com.ipachi.pos.dto.ModulePermissionDto;
import com.ipachi.pos.dto.RoleDto;
import com.ipachi.pos.model.Role;
import com.ipachi.pos.model.RoleModule;
import com.ipachi.pos.model.RolePermission;
import com.ipachi.pos.repo.RolePermissionRepository;
import com.ipachi.pos.repo.RoleRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import java.util.*;

/**
 * Handles CRUD for Roles + permissions with strict business scoping.
 *
 * Fixes "Duplicate entry '...-CASH_TILL' for key 'uq_role_perm_module'" on update by:
 *  - Fetching role with permissions in the same persistence context
 *  - Clearing the collection (orphanRemoval) and FLUSHING before inserting the new rows
 *  - Deduplicating incoming permissions per module
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {
    private final RoleRepository roles;
    private final RolePermissionRepository perms;
    private final CurrentRequest ctx;
    private final EntityManager em;

    private Long biz() {
        Long id = ctx.getBusinessId();
        if (id == null) throw new IllegalStateException("X-Business-Id missing");
        return id;
    }

    public RoleDto create(CreateRoleRequest req) {
        Long businessId = biz();

        String name = (req.name() == null) ? "" : req.name().trim();
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required");
        }
        if (roles.existsByBusinessIdAndNameIgnoreCase(businessId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
        }

        Role role = Role.builder()
                .businessId(businessId)
                .name(name)
                .build();

        // Deduplicate modules – last occurrence wins (UI should not send dups, but be defensive)
        Map<RoleModule, ModulePermissionDto> unique = uniqueByModule(req.permissions());

        List<RolePermission> toAttach = new ArrayList<>();
        for (ModulePermissionDto p : unique.values()) {
            if (p == null || p.module() == null) continue;
            toAttach.add(RolePermission.builder()
                    .role(role)
                    .module(p.module())
                    .create(p.create())
                    .view(p.view())
                    .edit(p.edit())
                    .delete(p.delete())
                    .build());
        }
        role.setPermissions(toAttach);

        Role saved = roles.save(role); // cascade persists permissions
        return toDtoWithPerms(saved, true);
    }

    public RoleDto update(Long id, CreateRoleRequest req) {
        Long businessId = biz();

        // Load role WITH permissions in current persistence context
        Role role = roles.findWithPermsByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        String name = (req.name() == null) ? "" : req.name().trim();
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required");
        }
        if (roles.existsByBusinessIdAndNameIgnoreCaseAndIdNot(businessId, name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
        }
        role.setName(name);

        // STEP 1: clear existing children (orphanRemoval=true) and FLUSH to write DELETEs first
        role.getPermissions().clear();
        roles.save(role);
        em.flush(); // ensures deletes hit DB before we insert replacements (avoids unique-key race)

        // STEP 2: attach NEW (deduped) permissions
        Map<RoleModule, ModulePermissionDto> unique = uniqueByModule(req.permissions());
        for (ModulePermissionDto p : unique.values()) {
            if (p == null || p.module() == null) continue;
            role.getPermissions().add(RolePermission.builder()
                    .role(role)
                    .module(p.module())
                    .create(p.create())
                    .view(p.view())
                    .edit(p.edit())
                    .delete(p.delete())
                    .build());
        }

        Role saved = roles.saveAndFlush(role);
        return toDtoWithPerms(saved, true);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> list() {
        Long businessId = biz();
        return roles.findByBusinessIdOrderByNameAsc(businessId).stream()
                .map(r -> toDtoWithPerms(r, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDto get(Long id) {
        Long businessId = biz();
        Role role = roles.findWithPermsByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        return toDtoWithPerms(role, true);
    }

    public void delete(Long id) {
        Long businessId = biz();
        Role role = roles.findWithPermsByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        // orphanRemoval on permissions ensures child rows are purged
        roles.delete(role);
    }

    /* ---------- helpers ---------- */

    private Map<RoleModule, ModulePermissionDto> uniqueByModule(List<ModulePermissionDto> in) {
        Map<RoleModule, ModulePermissionDto> map = new LinkedHashMap<>();
        if (in != null) {
            for (ModulePermissionDto p : in) {
                if (p == null || p.module() == null) continue;
                map.put(p.module(), p); // last wins
            }
        }
        return map;
    }

    private RoleDto toDtoWithPerms(Role r, boolean includePerms) {
        List<ModulePermissionDto> p = includePerms
                ? r.getPermissions().stream()
                .map(x -> new ModulePermissionDto(
                        x.getModule(),
                        x.isCreate(),
                        x.isView(),
                        x.isEdit(),
                        x.isDelete()))
                .toList()
                : List.of();

        return new RoleDto(r.getId(), r.getName(), r.getCreatedAt(), r.getUpdatedAt(), p);
    }
}
