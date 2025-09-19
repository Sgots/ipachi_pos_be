package com.ipachi.pos.model;


import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Entity
@Table(name = "terminals",
        uniqueConstraints = @UniqueConstraint(name = "uq_terminal_code_per_user", columnNames = {"user_id","code"}))
public class Terminal extends BaseOwnedEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String code;          // e.g. "TERM-1"

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 120)
    private String location;      // optional

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
