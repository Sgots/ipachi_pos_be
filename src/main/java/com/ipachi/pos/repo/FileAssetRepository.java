package com.ipachi.pos.repo;

import com.ipachi.pos.model.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileAssetRepository extends JpaRepository<FileAsset, String> {

    // Find file by ID and ensure it belongs to the user
    @Query("SELECT f FROM FileAsset f WHERE f.id = :id AND f.userId = :userId")
    Optional<FileAsset> findByIdAndUserId(@Param("id") String id, @Param("userId") Long userId);

    // Existing findById method for backward compatibility
    Optional<FileAsset> findById(String id);
}