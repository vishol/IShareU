package com.filesharing.model;

import java.time.LocalDateTime;

public class UploadResponse {
    private boolean success;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private String downloadUrl;
    private LocalDateTime uploadDate;
    private LocalDateTime expiryDate;
    private Integer maxDownloads;
    private String message;
    private FileRecord fileRecord;

    // Default constructor
    public UploadResponse() {
    }

    // Constructor with required fields
    public UploadResponse(String originalFilename, Long fileSize, String contentType, String downloadUrl) {
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.downloadUrl = downloadUrl;
        this.uploadDate = LocalDateTime.now();
        this.success = true;
    }

    // Constructor with all fields
    public UploadResponse(boolean success, String originalFilename, Long fileSize, String contentType, String downloadUrl, 
                         LocalDateTime expiryDate, Integer maxDownloads, String message, FileRecord fileRecord) {
        this(originalFilename, fileSize, contentType, downloadUrl);
        this.success = success;
        this.expiryDate = expiryDate;
        this.maxDownloads = maxDownloads;
        this.message = message;
        this.fileRecord = fileRecord;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public FileRecord getFileRecord() {
        return fileRecord;
    }

    public void setFileRecord(FileRecord fileRecord) {
        this.fileRecord = fileRecord;
    }

    // Business methods
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        
        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", (double) bytes, units[unitIndex]);
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    @Override
    public String toString() {
        return "UploadResponse{" +
                "success=" + success +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", contentType='" + contentType + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", uploadDate=" + uploadDate +
                ", expiryDate=" + expiryDate +
                ", maxDownloads=" + maxDownloads +
                ", message='" + message + '\'' +
                ", fileRecord=" + fileRecord +
                '}';
    }
} 