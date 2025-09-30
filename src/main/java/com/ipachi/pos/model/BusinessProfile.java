// src/main/java/com/ipachi/pos/model/BusinessProfile.java
package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "business_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessProfile {
    /** We will assign this manually in the service (no @GeneratedValue). */
    @Id
    @Column(nullable = false) // keep as BIGINT in DB
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String location;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_asset_id", unique = true)
    private FileAsset logoAsset;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = OffsetDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
