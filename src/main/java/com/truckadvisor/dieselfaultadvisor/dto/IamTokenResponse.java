package com.truckadvisor.dieselfaultadvisor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from IBM Cloud IAM token endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IamTokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("expiration")
    private Long expiration;
    
    @JsonProperty("scope")
    private String scope;
}

// Made with Bob
