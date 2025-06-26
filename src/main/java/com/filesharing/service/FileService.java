package com.filesharing.service;

import com.filesharing.dto.FileDownloadResponse;
import com.filesharing.dto.FileUploadRequest;
import com.filesharing.model.FileRecord;
import com.filesharing.model.UploadResponse;
import com.filesharing.repository.FileRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FileService {

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    /**
     * Save a file record to the database
     */
    public FileRecord saveFileRecord(FileRecord fileRecord) {
        return fileRecordRepository.save(fileRecord);
    }

    /**
     * Find file record by unique link
     */
    public Optional<FileRecord> findByUniqueLink(String uniqueLink) {
        return fileRecordRepository.findByUniqueLinkAndIsActiveTrue(uniqueLink);
    }

    /**
     * Find file record by ID
     */
    public Optional<FileRecord> findById(Long id) {
        return fileRecordRepository.findById(id);
    }

    /**
     * Get all active file records
     */
    public List<FileRecord> getAllActiveFiles() {
        return fileRecordRepository.findByIsActiveTrue();
    }

    /**
     * Get all file records by user ID
     */
    public List<FileRecord> getFilesByUserId(Long userId) {
        return fileRecordRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Delete a file record
     */
    public void deleteFileRecord(Long id) {
        fileRecordRepository.deleteById(id);
    }

    /**
     * Deactivate a file record (soft delete)
     */
    public void deactivateFileRecord(Long id) {
        Optional<FileRecord> fileRecord = fileRecordRepository.findById(id);
        if (fileRecord.isPresent()) {
            FileRecord record = fileRecord.get();
            record.setIsActive(false);
            fileRecordRepository.save(record);
        }
    }

    /**
     * Increment download count for a file
     */
    public void incrementDownloadCount(Long id) {
        Optional<FileRecord> fileRecord = fileRecordRepository.findById(id);
        if (fileRecord.isPresent()) {
            FileRecord record = fileRecord.get();
            record.incrementDownloadCount();
            fileRecordRepository.save(record);
        }
    }

    /**
     * Generate a unique link for file sharing
     */
    public String generateUniqueLink() {
        String uniqueLink;
        do {
            uniqueLink = UUID.randomUUID().toString().replace("-", "");
        } while (fileRecordRepository.existsByUniqueLink(uniqueLink));
        
        return uniqueLink;
    }

    /**
     * Create a file record from upload request
     */
    public FileRecord createFileRecord(FileUploadRequest uploadRequest, String storedFilename) {
        String uniqueLink = generateUniqueLink();
        
        FileRecord fileRecord = new FileRecord(
            uploadRequest.getOriginalFilename(),
            storedFilename,
            uploadRequest.getFileSize(),
            uploadRequest.getContentType(),
            uniqueLink
        );
        
        fileRecord.setMaxDownloads(uploadRequest.getMaxDownloads());
        fileRecord.setExpiryDate(uploadRequest.getExpiryDate());
        
        return fileRecord;
    }

    /**
     * Process file upload and return upload response
     */
    public UploadResponse processFileUpload(FileUploadRequest uploadRequest) {
        try {
            // Upload file to Supabase Storage
            String storedFilename = supabaseStorageService.uploadFile(uploadRequest.getFile());
            
            // Create file record
            FileRecord fileRecord = createFileRecord(uploadRequest, storedFilename);
            fileRecord = saveFileRecord(fileRecord);
            
            // Generate download URL
            String downloadUrl = supabaseStorageService.generateSignedUrl(storedFilename, java.time.Duration.ofHours(24));
            
            // Create upload response
            UploadResponse response = new UploadResponse(
                true, // success
                fileRecord.getOriginalFilename(),
                fileRecord.getFileSize(),
                fileRecord.getContentType(),
                downloadUrl,
                fileRecord.getExpiryDate(),
                fileRecord.getMaxDownloads(),
                "File uploaded successfully!",
                fileRecord
            );
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process file upload: " + e.getMessage(), e);
        }
    }

    /**
     * Process file download request
     */
    public FileDownloadResponse processFileDownload(String uniqueLink) {
        Optional<FileRecord> fileRecord = findByUniqueLink(uniqueLink);
        
        if (fileRecord.isEmpty()) {
            return FileDownloadResponse.createNotFoundResponse();
        }
        
        FileRecord record = fileRecord.get();
        
        // Check if file is expired
        if (record.isExpired()) {
            return FileDownloadResponse.createExpiredResponse();
        }
        
        // Check if download limit is reached
        if (record.hasReachedDownloadLimit()) {
            return FileDownloadResponse.createLimitReachedResponse();
        }
        
        // Check if file can be downloaded
        if (!record.canBeDownloaded()) {
            return new FileDownloadResponse("FILE_NOT_AVAILABLE", "File is not available for download.");
        }
        
        // Generate download URL
        String downloadUrl = supabaseStorageService.generateSignedUrl(record.getStoredFilename(), java.time.Duration.ofHours(1));
        
        // Create download response
        FileDownloadResponse response = new FileDownloadResponse(
            record.getOriginalFilename(),
            record.getFileSize(),
            record.getContentType(),
            record.getUploadDate(),
            record.getExpiryDate(),
            record.getDownloadCount(),
            record.getMaxDownloads(),
            downloadUrl
        );
        
        return response;
    }

    /**
     * Clean up expired files
     */
    public void cleanupExpiredFiles() {
        List<FileRecord> expiredFiles = fileRecordRepository.findExpiredFiles(LocalDateTime.now());
        for (FileRecord file : expiredFiles) {
            file.setIsActive(false);
            fileRecordRepository.save(file);
        }
    }

    /**
     * Get file statistics
     */
    public FileStatistics getFileStatistics() {
        long totalFiles = fileRecordRepository.countByIsActiveTrue();
        long expiredFiles = fileRecordRepository.findExpiredFiles(LocalDateTime.now()).size();
        long limitReachedFiles = fileRecordRepository.findFilesAtDownloadLimit().size();
        
        return new FileStatistics(totalFiles, expiredFiles, limitReachedFiles);
    }

    /**
     * Inner class for file statistics
     */
    public static class FileStatistics {
        private final long totalFiles;
        private final long expiredFiles;
        private final long limitReachedFiles;

        public FileStatistics(long totalFiles, long expiredFiles, long limitReachedFiles) {
            this.totalFiles = totalFiles;
            this.expiredFiles = expiredFiles;
            this.limitReachedFiles = limitReachedFiles;
        }

        public long getTotalFiles() {
            return totalFiles;
        }

        public long getExpiredFiles() {
            return expiredFiles;
        }

        public long getLimitReachedFiles() {
            return limitReachedFiles;
        }

        public long getActiveFiles() {
            return totalFiles - expiredFiles - limitReachedFiles;
        }
    }

    /**
     * Get all active file records by User entity
     */
    public List<FileRecord> findByUser(com.filesharing.model.User user) {
        if (user == null || user.getId() == null) return java.util.Collections.emptyList();
        return fileRecordRepository.findByUserIdAndIsActiveTrue(user.getId());
    }

    /**
     * Get paginated active file records by User entity
     */
    public org.springframework.data.domain.Page<FileRecord> findByUserPaginated(com.filesharing.model.User user, org.springframework.data.domain.Pageable pageable) {
        if (user == null || user.getId() == null) return org.springframework.data.domain.Page.empty();
        return fileRecordRepository.findByUserIdAndIsActiveTrue(user.getId(), pageable);
    }

    /**
     * Scheduled cleanup: deactivate expired or download-limit-reached files every hour
     */
    @Scheduled(cron = "0 0 * * * *") // every hour
    public void scheduledCleanup() {
        List<FileRecord> expiredOrLimitReached = fileRecordRepository.findExpiredOrLimitReachedFiles(java.time.LocalDateTime.now());
        for (FileRecord file : expiredOrLimitReached) {
            if (Boolean.TRUE.equals(file.getIsActive())) {
                file.setIsActive(false);
                fileRecordRepository.save(file);
            }
        }
    }

    public Page<FileRecord> getAllFilesPaginated(Pageable pageable) {
        return fileRecordRepository.findAll(pageable);
    }

    public Page<FileRecord> searchFilesByFilename(String filename, Pageable pageable) {
        return fileRecordRepository.findByOriginalFilenameContainingIgnoreCase(filename, pageable);
    }
} 