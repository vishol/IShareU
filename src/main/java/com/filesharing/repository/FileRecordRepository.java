package com.filesharing.repository;

import com.filesharing.model.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    /**
     * Find file record by unique link
     */
    Optional<FileRecord> findByUniqueLink(String uniqueLink);

    /**
     * Find file record by unique link and check if it's active
     */
    Optional<FileRecord> findByUniqueLinkAndIsActiveTrue(String uniqueLink);

    /**
     * Find all active file records
     */
    List<FileRecord> findByIsActiveTrue();

    /**
     * Find all file records by user ID
     */
    List<FileRecord> findByUserId(Long userId);

    /**
     * Find all active file records by user ID
     */
    List<FileRecord> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find all active file records by user ID (paginated)
     */
    org.springframework.data.domain.Page<FileRecord> findByUserIdAndIsActiveTrue(Long userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Find expired file records
     */
    @Query("SELECT f FROM FileRecord f WHERE f.expiryDate IS NOT NULL AND f.expiryDate < :now")
    List<FileRecord> findExpiredFiles(@Param("now") LocalDateTime now);

    /**
     * Find file records that have reached their download limit
     */
    @Query("SELECT f FROM FileRecord f WHERE f.downloadCount >= f.maxDownloads")
    List<FileRecord> findFilesAtDownloadLimit();

    /**
     * Find file records that are expired or at download limit
     */
    @Query("SELECT f FROM FileRecord f WHERE (f.expiryDate IS NOT NULL AND f.expiryDate < :now) OR f.downloadCount >= f.maxDownloads")
    List<FileRecord> findExpiredOrLimitReachedFiles(@Param("now") LocalDateTime now);

    /**
     * Count active file records
     */
    long countByIsActiveTrue();

    /**
     * Count file records by user ID
     */
    long countByUserId(Long userId);

    /**
     * Count active file records by user ID
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find file records uploaded in the last 24 hours
     */
    @Query("SELECT f FROM FileRecord f WHERE f.uploadDate >= :startDate")
    List<FileRecord> findRecentUploads(@Param("startDate") LocalDateTime startDate);

    /**
     * Find file records by content type
     */
    List<FileRecord> findByContentType(String contentType);

    /**
     * Find file records by content type and active status
     */
    List<FileRecord> findByContentTypeAndIsActiveTrue(String contentType);

    /**
     * Delete expired file records
     */
    @Query("DELETE FROM FileRecord f WHERE f.expiryDate IS NOT NULL AND f.expiryDate < :now")
    void deleteExpiredFiles(@Param("now") LocalDateTime now);

    /**
     * Check if unique link exists
     */
    boolean existsByUniqueLink(String uniqueLink);

    /**
     * Find file records with download count greater than specified value
     */
    List<FileRecord> findByDownloadCountGreaterThan(Integer downloadCount);

    /**
     * Find file records by file size range
     */
    @Query("SELECT f FROM FileRecord f WHERE f.fileSize BETWEEN :minSize AND :maxSize")
    List<FileRecord> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * Find file records by original filename (paginated)
     */
    org.springframework.data.domain.Page<FileRecord> findByOriginalFilenameContainingIgnoreCase(String filename, org.springframework.data.domain.Pageable pageable);
} 