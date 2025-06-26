package com.filesharing.controller;

import com.filesharing.dto.FileUploadRequest;
import com.filesharing.model.UploadResponse;
import com.filesharing.model.FileRecord;
import com.filesharing.service.FileService;
import com.filesharing.service.SupabaseStorageService;
import com.filesharing.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import com.filesharing.config.AllowedFileTypesProperties;

@Controller
@RequestMapping("/file")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private UserService userService;

    @Autowired
    private AllowedFileTypesProperties allowedFileTypesProperties;

    private final Map<String, Bucket> uploadBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> downloadBuckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(Map<String, Bucket> buckets, String key, long capacity, long refillTokens, Duration refillDuration) {
        return buckets.computeIfAbsent(key, k -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillDuration)))
                .build());
    }

    /**
     * Show the file upload form
     */
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("uploadRequest", new FileUploadRequest());
        return "upload";
    }

    /**
     * Handle file upload
     */
    @PostMapping("/upload")
    public String uploadFile(@Valid @ModelAttribute("uploadRequest") FileUploadRequest uploadRequest,
                           BindingResult bindingResult,
                           @RequestParam("file") MultipartFile[] files,
                           Model model,
                           RedirectAttributes redirectAttributes,
                           @AuthenticationPrincipal UserDetails userDetails,
                           HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        Bucket bucket = resolveBucket(uploadBuckets, ip, 10, 10, Duration.ofMinutes(1));
        if (!bucket.tryConsume(1)) {
            bindingResult.rejectValue("file", "error.file", "Too many upload requests. Please wait and try again.");
            return "upload";
        }
        String userEmail = (userDetails != null) ? userDetails.getUsername() : "anonymous";
        logger.info("UPLOAD_ATTEMPT: user={}, ip={}, files={} ({} files)", userEmail, ip, files != null ? java.util.Arrays.stream(files).map(f -> f.getOriginalFilename()).toList() : "none", files != null ? files.length : 0);
        try {
            if (files == null || files.length == 0) {
                bindingResult.rejectValue("file", "error.file", "Please select at least one file to upload");
                return "upload";
            }

            java.util.List<UploadResponse> responses = new java.util.ArrayList<>();
            com.filesharing.model.User user = null;
            if (userDetails != null) {
                user = userService.findByEmail(userDetails.getUsername())
                        .orElseGet(() -> userService.getUserRepository().findByUsername(userDetails.getUsername()).orElse(null));
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    UploadResponse resp = new UploadResponse();
                    resp.setSuccess(false);
                    resp.setMessage("No file selected.");
                    responses.add(resp);
                    continue;
                }
                String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
                String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
                boolean allowed = allowedFileTypesProperties.getAllowedFileTypes().stream().anyMatch(type ->
                    contentType.contains(type) || filename.endsWith("." + type)
                );
                if (!allowed) {
                    UploadResponse resp = new UploadResponse();
                    resp.setSuccess(false);
                    resp.setMessage("File type not allowed: " + contentType + " (" + filename + ")");
                    resp.setFileRecord(new FileRecord());
                    resp.getFileRecord().setOriginalFilename(file.getOriginalFilename());
                    responses.add(resp);
                    continue;
                }
                if (file.getSize() > 1024 * 1024 * 1024) {
                    UploadResponse resp = new UploadResponse();
                    resp.setSuccess(false);
                    resp.setMessage("File size cannot exceed 1GB.");
                    resp.setFileRecord(new FileRecord());
                    resp.getFileRecord().setOriginalFilename(file.getOriginalFilename());
                    responses.add(resp);
                    continue;
                }
                if (file.getOriginalFilename() != null && file.getOriginalFilename().trim().isEmpty()) {
                    UploadResponse resp = new UploadResponse();
                    resp.setSuccess(false);
                    resp.setMessage("Invalid filename.");
                    responses.add(resp);
                    continue;
                }
                try {
                    String storedFilename = supabaseStorageService.uploadFile(file);
                    FileRecord fileRecord = new FileRecord();
                    fileRecord.setOriginalFilename(file.getOriginalFilename());
                    fileRecord.setStoredFilename(storedFilename);
                    fileRecord.setFileSize(file.getSize());
                    fileRecord.setContentType(file.getContentType());
                    fileRecord.setUploadDate(LocalDateTime.now());
                    fileRecord.setUniqueLink(generateUniqueLink());
                    fileRecord.setDownloadCount(0);
                    fileRecord.setMaxDownloads(uploadRequest.getMaxDownloads() != null ? uploadRequest.getMaxDownloads() : 1);
                    fileRecord.setIsActive(true);
                    if (user != null) {
                        fileRecord.setUserId(user.getId());
                    }
                    if (uploadRequest.getExpiryHours() != null && uploadRequest.getExpiryHours() > 0) {
                        fileRecord.setExpiryDate(LocalDateTime.now().plusHours(uploadRequest.getExpiryHours()));
                    }
                    FileRecord savedRecord = fileService.saveFileRecord(fileRecord);
                    UploadResponse uploadResponse = new UploadResponse();
                    uploadResponse.setSuccess(true);
                    uploadResponse.setMessage("File uploaded successfully!");
                    uploadResponse.setDownloadUrl("/file/download/" + savedRecord.getUniqueLink());
                    uploadResponse.setFileRecord(savedRecord);
                    responses.add(uploadResponse);
                    logger.info("UPLOAD_SUCCESS: user={}, ip={}, file={}, size={} bytes", userEmail, ip, file.getOriginalFilename(), file.getSize());
                } catch (Exception ex) {
                    UploadResponse resp = new UploadResponse();
                    resp.setSuccess(false);
                    resp.setMessage("Error uploading file: " + ex.getMessage());
                    resp.setFileRecord(new FileRecord());
                    resp.getFileRecord().setOriginalFilename(file.getOriginalFilename());
                    responses.add(resp);
                    logger.warn("UPLOAD_FAIL: user={}, ip={}, file={}, reason={}", userEmail, ip, file.getOriginalFilename(), ex.getMessage());
                }
            }
            model.addAttribute("uploadResponses", responses);
            return "upload-success";
        } catch (Exception e) {
            bindingResult.rejectValue("file", "error.file", "Unexpected error: " + e.getMessage());
            return "upload";
        }
    }

    /**
     * Show download page
     */
    @GetMapping("/download/{uniqueLink}")
    public String showDownloadPage(@PathVariable String uniqueLink, Model model) {
        logger.debug("Accessing download page for link: {}", uniqueLink);
        try {
            FileRecord fileRecord = fileService.findByUniqueLink(uniqueLink).orElse(null);
            if (fileRecord == null) {
                logger.debug("File not found for link: {}", uniqueLink);
                model.addAttribute("error", "File not found or link is invalid");
                return "error";
            }
            logger.debug("Found file: {}", fileRecord.getOriginalFilename());
            if (!fileRecord.getIsActive()) {
                logger.debug("File is not active");
                model.addAttribute("error", "This file is no longer available");
                return "error";
            }
            if (fileRecord.getExpiryDate() != null && LocalDateTime.now().isAfter(fileRecord.getExpiryDate())) {
                logger.debug("File has expired");
                model.addAttribute("error", "This download link has expired");
                return "error";
            }
            if (fileRecord.getDownloadCount() >= fileRecord.getMaxDownloads()) {
                logger.debug("Download limit reached");
                model.addAttribute("error", "Maximum download limit reached for this file");
                return "error";
            }
            logger.debug("File is available for download");
            model.addAttribute("fileRecord", fileRecord);
            return "download";
        } catch (Exception e) {
            logger.error("Exception occurred: {}", e.getMessage(), e);
            model.addAttribute("error", "Error accessing file: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Handle file download
     */
    @GetMapping("/download/{uniqueLink}/file")
    public String downloadFile(@PathVariable String uniqueLink, Model model, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        Bucket bucket = resolveBucket(downloadBuckets, ip, 20, 20, Duration.ofMinutes(1));
        if (!bucket.tryConsume(1)) {
            model.addAttribute("error", "Too many download requests. Please wait and try again.");
            return "error";
        }
        logger.info("DOWNLOAD_ATTEMPT: ip={}, link={}", ip, uniqueLink);
        try {
            FileRecord fileRecord = fileService.findByUniqueLink(uniqueLink).orElse(null);
            if (fileRecord == null) {
                logger.debug("File not found for link: {}", uniqueLink);
                model.addAttribute("error", "File not found");
                return "error";
            }
            // Validate access
            if (!fileRecord.getIsActive() || 
                (fileRecord.getExpiryDate() != null && LocalDateTime.now().isAfter(fileRecord.getExpiryDate())) ||
                fileRecord.getDownloadCount() >= fileRecord.getMaxDownloads()) {
                logger.debug("File access denied for link: {}", uniqueLink);
                model.addAttribute("error", "File access denied");
                return "error";
            }
            // Generate signed URL for download
            String signedUrl = supabaseStorageService.generateSignedUrl(fileRecord.getStoredFilename(), 
                                                                       java.time.Duration.ofHours(1));
            logger.debug("Signed URL generated: {}", signedUrl);
            // Increment download count
            fileService.incrementDownloadCount(fileRecord.getId());
            logger.debug("Download count incremented for file: {}", fileRecord.getId());
            logger.info("DOWNLOAD_SUCCESS: ip={}, file={}, id={}", ip, fileRecord.getOriginalFilename(), fileRecord.getId());
            // Redirect to signed URL
            return "redirect:" + signedUrl;
        } catch (Exception e) {
            logger.error("Exception during download: {}", e.getMessage(), e);
            model.addAttribute("error", "Error downloading file: " + e.getMessage());
            logger.warn("DOWNLOAD_FAIL: ip={}, link={}, reason={}", ip, uniqueLink, model.getAttribute("error"));
            return "error";
        }
    }

    /**
     * Generate a unique download link
     */
    private String generateUniqueLink() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Show edit file form
     */
    @GetMapping("/edit/{id}")
    public String showEditFileForm(@PathVariable Long id, Model model) {
        FileRecord fileRecord = fileService.findById(id).orElse(null);
        if (fileRecord == null) {
            model.addAttribute("error", "File not found");
            return "error";
        }
        model.addAttribute("fileRecord", fileRecord);
        return "edit-file";
    }

    /**
     * Handle edit file POST
     */
    @PostMapping("/edit/{id}")
    public String editFile(@PathVariable Long id,
                          @RequestParam("originalFilename") String originalFilename,
                          @RequestParam("maxDownloads") Integer maxDownloads,
                          @RequestParam(value = "expiryDate", required = false) String expiryDateStr,
                          RedirectAttributes redirectAttributes) {
        FileRecord fileRecord = fileService.findById(id).orElse(null);
        if (fileRecord == null) {
            redirectAttributes.addFlashAttribute("error", "File not found");
            return "redirect:/user/details";
        }
        fileRecord.setOriginalFilename(originalFilename);
        fileRecord.setMaxDownloads(maxDownloads);
        if (expiryDateStr != null && !expiryDateStr.isBlank()) {
            java.time.LocalDateTime expiryDate = java.time.LocalDateTime.parse(expiryDateStr);
            fileRecord.setExpiryDate(expiryDate);
        } else {
            fileRecord.setExpiryDate(null);
        }
        fileService.saveFileRecord(fileRecord);
        redirectAttributes.addFlashAttribute("success", "File updated successfully!");
        return "redirect:/user/details";
    }

    /**
     * Delete a single uploaded file
     */
    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        fileService.deleteFileRecord(id);
        redirectAttributes.addFlashAttribute("success", "File deleted successfully!");
        return "redirect:/user/details";
    }

    /**
     * Delete all uploaded files for the current user
     */
    @PostMapping("/delete/all")
    public String deleteAllFiles(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails != null) {
            com.filesharing.model.User user = userService.findByEmail(userDetails.getUsername())
                .orElseGet(() -> userService.getUserRepository().findByUsername(userDetails.getUsername()).orElse(null));
            if (user != null) {
                java.util.List<FileRecord> files = fileService.findByUser(user);
                for (FileRecord file : files) {
                    fileService.deleteFileRecord(file.getId());
                }
            }
        }
        redirectAttributes.addFlashAttribute("success", "All uploaded files deleted!");
        return "redirect:/user/details";
    }
} 