package com.filesharing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public class FileUploadRequest {

    @NotNull(message = "File is required")
    private MultipartFile file;

    @Min(value = 1, message = "Max downloads must be at least 1")
    @Max(value = 100, message = "Max downloads cannot exceed 100")
    private Integer maxDownloads = 1;

    @Min(value = 0, message = "Expiry hours must be 0 or positive")
    @Max(value = 168, message = "Expiry hours cannot exceed 168 (1 week)")
    private Integer expiryHours = 24;

    private LocalDateTime expiryDate;

    private String description;

    // Default constructor
    public FileUploadRequest() {
    }

    // Constructor with required fields
    public FileUploadRequest(MultipartFile file) {
        this.file = file;
    }

    // Constructor with all fields
    public FileUploadRequest(MultipartFile file, Integer maxDownloads, Integer expiryHours, LocalDateTime expiryDate, String description) {
        this.file = file;
        this.maxDownloads = maxDownloads != null ? maxDownloads : 1;
        this.expiryHours = expiryHours != null ? expiryHours : 24;
        this.expiryDate = expiryDate;
        this.description = description;
    }

    // Getters and Setters
    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads != null ? maxDownloads : 1;
    }

    public Integer getExpiryHours() {
        return expiryHours;
    }

    public void setExpiryHours(Integer expiryHours) {
        this.expiryHours = expiryHours != null ? expiryHours : 24;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Business methods
    public boolean hasFile() {
        return file != null && !file.isEmpty();
    }

    public String getOriginalFilename() {
        return file != null ? file.getOriginalFilename() : null;
    }

    public long getFileSize() {
        return file != null ? file.getSize() : 0;
    }

    public String getContentType() {
        return file != null ? file.getContentType() : null;
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isValidFileSize(long maxSizeInBytes) {
        return file != null && file.getSize() <= maxSizeInBytes;
    }

    public boolean isValidFileType(String[] allowedTypes) {
        if (file == null || file.getContentType() == null) {
            return false;
        }
        
        String contentType = file.getContentType().toLowerCase();
        for (String allowedType : allowedTypes) {
            if (contentType.startsWith(allowedType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "FileUploadRequest{" +
                "originalFilename='" + getOriginalFilename() + '\'' +
                ", fileSize=" + getFileSize() +
                ", contentType='" + getContentType() + '\'' +
                ", maxDownloads=" + maxDownloads +
                ", expiryHours=" + expiryHours +
                ", expiryDate=" + expiryDate +
                ", description='" + description + '\'' +
                '}';
    }
} 