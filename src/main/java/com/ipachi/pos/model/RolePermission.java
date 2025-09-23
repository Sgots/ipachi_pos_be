// src/main/java/com/ipachi/pos/model/RolePermission.java
package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "staff_role_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_role_perm_module", columnNames = {"role_id", "module"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoleModule module;

    @Column(name = "can_create", nullable = false)
    private boolean create;

    @Column(name = "can_view", nullable = false)
    private boolean view;

    @Column(name = "can_edit", nullable = false)
    private boolean edit;

    @Column(name = "can_delete", nullable = false)
    private boolean delete;
}
