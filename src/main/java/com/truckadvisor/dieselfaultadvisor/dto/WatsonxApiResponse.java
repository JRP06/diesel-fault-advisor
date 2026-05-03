package com.truckadvisor.dieselfaultadvisor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO from watsonx.ai API
 * Follows IBM watsonx.ai REST API specification for Granite models
 * Uses @JsonIgnoreProperties to handle unknown fields gracefully
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WatsonxApiResponse {
    
    @JsonProperty("model_id")
    private String modelId;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    private List<Result> results;
    
    @JsonProperty("system")
    private SystemInfo system;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        
        @JsonProperty("generated_text")
        private String generatedText;
        
        @JsonProperty("generated_token_count")
        private Integer generatedTokenCount;
        
        @JsonProperty("input_token_count")
        private Integer inputTokenCount;
        
        @JsonProperty("stop_reason")
        private String stopReason;
        
        @JsonProperty("seed")
        private Long seed;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemInfo {
        private Object warnings;
    }
}
