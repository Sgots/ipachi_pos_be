// src/main/java/com/ipachi/pos/model/Category.java
package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inv_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inv_category_name_per_business", columnNames = {"business_id", "name"}))
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 64)
    private String name;
}
