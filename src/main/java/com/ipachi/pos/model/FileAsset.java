package com.ipachi.pos.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;

@Entity @Table(name = "file_assets")
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileAsset extends BaseOwnedEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;                       // UUID string

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Lob
    @Basic(fetch = FetchType.LAZY)           // don't pull blob unless requested
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] data;

    private OffsetDateTime createdAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }
}
