package com.filesharing.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SupabaseConfig {

    @Value("${supabase.storage.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.api-key}")
    private String apiKey;

    @Value("${supabase.storage.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    @Bean
    public CloseableHttpClient httpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public String getBucket() {
        return bucket;
    }
} 