package com.filesharing.service;

import com.filesharing.config.SupabaseConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Autowired
    private SupabaseConfig supabaseConfig;

    @Autowired
    private CloseableHttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Upload a file to Supabase Storage
     */
    public String uploadFile(MultipartFile file) throws IOException {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Build the upload URL
            String uploadUrl = supabaseConfig.getSupabaseUrl() + "/object/" + supabaseConfig.getBucket() + "/" + uniqueFilename;
            
            // Create HTTP POST request
            HttpPost httpPost = new HttpPost(uploadUrl);
            httpPost.setHeader("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey());
            
            // Build multipart entity
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", file.getInputStream(), ContentType.create(file.getContentType()), uniqueFilename);
            
            httpPost.setEntity(builder.build());
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                
                if (statusCode == 200 || statusCode == 201) {
                    return uniqueFilename;
                } else {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    throw new RuntimeException("Failed to upload file to Supabase. Status: " + statusCode + ", Response: " + responseBody);
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to Supabase Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a signed URL for file download
     */
    public String generateSignedUrl(String filename, Duration expiry) {
        try {
            // Build the signed URL request
            String signedUrlEndpoint = supabaseConfig.getSupabaseUrl() + "/object/sign/" + supabaseConfig.getBucket() + "/" + filename;
            
            // Create HTTP POST request (Supabase expects POST with JSON body)
            HttpPost httpPost = new HttpPost(signedUrlEndpoint);
            httpPost.setHeader("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey());
            httpPost.setHeader("Content-Type", "application/json");

            // Set JSON body with expiresIn (in seconds)
            int expiresIn = (int) expiry.getSeconds();
            String jsonBody = "{\"expiresIn\":" + expiresIn + "}";
            httpPost.setEntity(new org.apache.hc.core5.http.io.entity.StringEntity(jsonBody, org.apache.hc.core5.http.ContentType.APPLICATION_JSON));

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return extractSignedUrlFromResponse(responseBody);
                } else {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    throw new RuntimeException("Failed to generate signed URL. Status: " + statusCode + ", Response: " + responseBody);
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating signed URL: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Supabase Storage
     */
    public void deleteFile(String filename) {
        try {
            // Build the delete URL
            String deleteUrl = supabaseConfig.getSupabaseUrl() + "/object/" + supabaseConfig.getBucket() + "/" + filename;
            
            // Create HTTP DELETE request (using POST with _method=DELETE for compatibility)
            HttpPost httpPost = new HttpPost(deleteUrl);
            httpPost.setHeader("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey());
            httpPost.setHeader("X-HTTP-Method-Override", "DELETE");
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                
                if (statusCode != 200 && statusCode != 204) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    throw new RuntimeException("Failed to delete file from Supabase. Status: " + statusCode + ", Response: " + responseBody);
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from Supabase Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Get file information from Supabase Storage
     */
    public FileInfo getFileInfo(String filename) {
        try {
            // Build the file info URL
            String fileInfoUrl = supabaseConfig.getSupabaseUrl() + "/object/info/" + supabaseConfig.getBucket() + "/" + filename;
            
            // Create HTTP GET request
            HttpGet httpGet = new HttpGet(fileInfoUrl);
            httpGet.setHeader("Authorization", "Bearer " + supabaseConfig.getApiKey());
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getCode();
                
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return parseFileInfoFromResponse(responseBody);
                } else {
                    return null; // File not found
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error getting file info from Supabase Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a file exists in Supabase Storage
     */
    public boolean fileExists(String filename) {
        return getFileInfo(filename) != null;
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * Extract signed URL from Supabase response
     */
    private String extractSignedUrlFromResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String signedUrl = null;
            if (jsonNode.has("signedURL")) {
                signedUrl = jsonNode.get("signedURL").asText();
            } else if (jsonNode.has("data") && jsonNode.get("data").has("signedURL")) {
                signedUrl = jsonNode.get("data").get("signedURL").asText();
            }
            if (signedUrl != null && !signedUrl.startsWith("http")) {
                // Prepend the Supabase Storage base URL
                return supabaseConfig.getSupabaseUrl() + signedUrl;
            }
            return signedUrl != null ? signedUrl : responseBody;
        } catch (Exception e) {
            // Fallback to direct response if JSON parsing fails
            return responseBody;
        }
    }

    /**
     * Parse file info from Supabase response
     */
    private FileInfo parseFileInfoFromResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String name = jsonNode.has("name") ? jsonNode.get("name").asText() : "unknown";
            Long size = jsonNode.has("size") ? jsonNode.get("size").asLong() : 0L;
            String contentType = jsonNode.has("mimeType") ? jsonNode.get("mimeType").asText() : "application/octet-stream";
            return new FileInfo(name, size, contentType);
        } catch (Exception e) {
            // Fallback to default values if JSON parsing fails
            return new FileInfo("unknown", 0L, "application/octet-stream");
        }
    }

    /**
     * Inner class for file information
     */
    public static class FileInfo {
        private final String name;
        private final Long size;
        private final String contentType;

        public FileInfo(String name, Long size, String contentType) {
            this.name = name;
            this.size = size;
            this.contentType = contentType;
        }

        public String getName() {
            return name;
        }

        public Long getSize() {
            return size;
        }

        public String getContentType() {
            return contentType;
        }
    }
} 