package com.ipachi.pos.model;

// src/main/java/com/ipachi/pos/inventory/entity/Category.java

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity @Table(name = "inv_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inv_category_name", columnNames = "name"))
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@AllArgsConstructor// REQUIRED: This provides the default constructor for Hibernate
public class Category  extends BaseOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;


}
