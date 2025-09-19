package com.ipachi.pos.repo;

import com.ipachi.pos.model.StockReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {

    /**
     * Find receipt by ID and user ID (used in getFile)
     */
    @Query("SELECT r FROM StockReceipt r WHERE r.id = :id AND r.userId = :userId")
    Optional<StockReceipt> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Find top 50 receipts by user ID, ordered by createdAt DESC (used in search without query)
     */
    @Query("SELECT r FROM StockReceipt r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<StockReceipt> findTop50ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * FIXED: Search receipts by reference for a specific user
     * Simplified to search only in reference field to avoid JPQL complexity
     */

    /**
     * Alternative: Search across multiple fields if needed
     */
    @Query("SELECT r FROM StockReceipt r WHERE r.userId = :userId AND " +
            "(LOWER(r.label) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(r.fileName, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<StockReceipt> searchAndUserId(@Param("query") String query, @Param("userId") Long userId);

    /**
     * Find receipts by user ID (used in receipts endpoint without search)
     */
    @Query("SELECT r FROM StockReceipt r WHERE r.userId = :userId")
    Page<StockReceipt> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Search receipts by reference and user ID (used in receipts endpoint with search)
     */


    /**
     * Check if reference exists for a user (for uniqueness validation)
     */

    /**
     * Find receipts by user ID ordered by createdAt (most recent first)
     */
    @Query("SELECT r FROM StockReceipt r WHERE r.userId = :userId ORDER BY r.createdAt DESC, r.createdAt DESC")
    Page<StockReceipt> findByUserIdOrderBycreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}