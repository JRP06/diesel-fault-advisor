package com.truckadvisor.dieselfaultadvisor.service;

import com.truckadvisor.dieselfaultadvisor.config.WatsonxConfig;
import com.truckadvisor.dieselfaultadvisor.dto.IamTokenResponse;
import com.truckadvisor.dieselfaultadvisor.exception.WatsonxApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

/**
 * Service for managing IBM Cloud IAM authentication tokens
 * Handles token exchange and caching with automatic refresh
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IamTokenService {
    
    private static final String IAM_TOKEN_URL = "https://iam.cloud.ibm.com/identity/token";
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300; // Refresh 5 minutes before expiry
    
    private final WatsonxConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Cached token information
    private String cachedAccessToken;
    private Long tokenExpirationTime;
    
    /**
     * Get a valid IAM access token
     * Returns cached token if still valid, otherwise requests a new one
     * 
     * @return Valid IAM access token
     */
    public synchronized String getAccessToken() {
        if (isTokenValid()) {
            log.debug("Using cached IAM token");
            return cachedAccessToken;
        }
        
        log.info("Requesting new IAM token");
        return requestNewToken();
    }
    
    /**
     * Check if the cached token is still valid
     */
    private boolean isTokenValid() {
        if (cachedAccessToken == null || tokenExpirationTime == null) {
            return false;
        }
        
        long currentTime = Instant.now().getEpochSecond();
        long timeUntilExpiry = tokenExpirationTime - currentTime;
        
        // Token is valid if it won't expire within the buffer period
        return timeUntilExpiry > TOKEN_REFRESH_BUFFER_SECONDS;
    }
    
    /**
     * Request a new IAM token from IBM Cloud
     */
    private String requestNewToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth("bx", "bx"); // IBM Cloud standard credentials
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "urn:ibm:params:oauth:grant-type:apikey");
            body.add("apikey", config.getApiKey());
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            log.debug("Calling IAM token endpoint: {}", IAM_TOKEN_URL);
            ResponseEntity<IamTokenResponse> response = restTemplate.exchange(
                IAM_TOKEN_URL,
                HttpMethod.POST,
                request,
                IamTokenResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                IamTokenResponse tokenResponse = response.getBody();
                
                // Cache the token and expiration time
                cachedAccessToken = tokenResponse.getAccessToken();
                tokenExpirationTime = tokenResponse.getExpiration();
                
                long expiresInMinutes = tokenResponse.getExpiresIn() / 60;
                log.info("Successfully obtained IAM token (expires in {} minutes)", expiresInMinutes);
                
                return cachedAccessToken;
            } else {
                throw new WatsonxApiException("Failed to obtain IAM token: Unexpected response");
            }
            
        } catch (Exception e) {
            log.error("Error obtaining IAM token", e);
            
            // Clear cached token on error
            cachedAccessToken = null;
            tokenExpirationTime = null;
            
            throw new WatsonxApiException("Failed to obtain IAM token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Force refresh of the cached token
     * Useful for testing or when token becomes invalid
     */
    public synchronized void refreshToken() {
        log.info("Forcing IAM token refresh");
        cachedAccessToken = null;
        tokenExpirationTime = null;
        getAccessToken();
    }
    
    /**
     * Clear the cached token
     */
    public synchronized void clearToken() {
        log.info("Clearing cached IAM token");
        cachedAccessToken = null;
        tokenExpirationTime = null;
    }
}

// Made with Bob
