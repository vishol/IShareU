package com.filesharing.repository;

import com.filesharing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users created between two dates
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count users created in the last 24 hours
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countRecentUsers(@Param("startDate") LocalDateTime startDate);

    /**
     * Find users with email containing specific text
     */
    List<User> findByEmailContainingIgnoreCase(String email);

    /**
     * Find users by email pattern
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE %:emailPattern%")
    List<User> findByEmailPattern(@Param("emailPattern") String emailPattern);

    /**
     * Find users who have uploaded files
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.fileRecords f")
    List<User> findUsersWithFiles();

    /**
     * Find users who have active files
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.fileRecords f WHERE f.isActive = true")
    List<User> findUsersWithActiveFiles();

    /**
     * Find users with file count greater than specified value
     */
    @Query("SELECT u FROM User u WHERE SIZE(u.fileRecords) > :fileCount")
    List<User> findUsersWithFileCountGreaterThan(@Param("fileCount") int fileCount);

    /**
     * Find users with total file size greater than specified value
     */
    @Query("SELECT u FROM User u JOIN u.fileRecords f GROUP BY u HAVING SUM(f.fileSize) > :totalSize")
    List<User> findUsersWithTotalFileSizeGreaterThan(@Param("totalSize") Long totalSize);

    /**
     * Find top users by file count
     */
    @Query("SELECT u FROM User u JOIN u.fileRecords f GROUP BY u ORDER BY COUNT(f) DESC")
    List<User> findTopUsersByFileCount();

    /**
     * Find top users by total file size
     */
    @Query("SELECT u FROM User u JOIN u.fileRecords f GROUP BY u ORDER BY SUM(f.fileSize) DESC")
    List<User> findTopUsersByFileSize();

    /**
     * Delete users with no files
     */
    @Query("DELETE FROM User u WHERE SIZE(u.fileRecords) = 0")
    void deleteUsersWithNoFiles();

    /**
     * Find users created in the last month
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :monthAgo")
    List<User> findUsersCreatedInLastMonth(@Param("monthAgo") LocalDateTime monthAgo);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
} 