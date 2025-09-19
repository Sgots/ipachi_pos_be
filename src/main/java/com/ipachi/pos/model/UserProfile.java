package com.ipachi.pos.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity @Table(name = "user_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true, nullable = false)
    private User user;

    private String title;           // Mr/Ms/Dr
    private String gender;          // store as text (Male/Female/Other)
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private IdType idType;          // NATIONAL_ID / PASSPORT / DRIVER_LICENSE

    private String idNumber;

    private String postalAddress;
    private String physicalAddress;
    private String city;
    private String country;
    private String areaCode;
    private String phone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picture_asset_id", unique = true)
    private FileAsset pictureAsset;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_doc_asset_id", unique = true)
    private FileAsset idDocAsset;


    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = OffsetDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = OffsetDateTime.now(); }
}

