package com.filesharing.dto;

import java.time.LocalDateTime;

public class FileDownloadResponse {
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadDate;
    private LocalDateTime expiryDate;
    private Integer downloadCount;
    private Integer maxDownloads;
    private String downloadUrl;
    private boolean canDownload;
    private String message;
    private String error;

    // Default constructor
    public FileDownloadResponse() {
    }

    // Constructor for successful response
    public FileDownloadResponse(String originalFilename, Long fileSize, String contentType, 
                               LocalDateTime uploadDate, LocalDateTime expiryDate, 
                               Integer downloadCount, Integer maxDownloads, String downloadUrl) {
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadDate = uploadDate;
        this.expiryDate = expiryDate;
        this.downloadCount = downloadCount;
        this.maxDownloads = maxDownloads;
        this.downloadUrl = downloadUrl;
        this.canDownload = true;
        this.message = "File is available for download";
    }

    // Constructor for error response
    public FileDownloadResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.canDownload = false;
    }

    // Getters and Setters
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

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isCanDownload() {
        return canDownload;
    }

    public void setCanDownload(boolean canDownload) {
        this.canDownload = canDownload;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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

    public boolean hasReachedDownloadLimit() {
        return downloadCount != null && maxDownloads != null && downloadCount >= maxDownloads;
    }

    public int getRemainingDownloads() {
        if (downloadCount == null || maxDownloads == null) {
            return 0;
        }
        return Math.max(0, maxDownloads - downloadCount);
    }

    public String getStatusMessage() {
        if (isExpired()) {
            return "This file has expired and is no longer available for download.";
        } else if (hasReachedDownloadLimit()) {
            return "This file has reached its download limit and is no longer available.";
        } else if (canDownload) {
            return "File is available for download.";
        } else {
            return message != null ? message : "File is not available for download.";
        }
    }

    public static FileDownloadResponse createExpiredResponse() {
        return new FileDownloadResponse("FILE_EXPIRED", "This file has expired and is no longer available for download.");
    }

    public static FileDownloadResponse createLimitReachedResponse() {
        return new FileDownloadResponse("DOWNLOAD_LIMIT_REACHED", "This file has reached its download limit and is no longer available.");
    }

    public static FileDownloadResponse createNotFoundResponse() {
        return new FileDownloadResponse("FILE_NOT_FOUND", "The requested file was not found or is no longer available.");
    }

    @Override
    public String toString() {
        return "FileDownloadResponse{" +
                "originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", contentType='" + contentType + '\'' +
                ", uploadDate=" + uploadDate +
                ", expiryDate=" + expiryDate +
                ", downloadCount=" + downloadCount +
                ", maxDownloads=" + maxDownloads +
                ", canDownload=" + canDownload +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
} 