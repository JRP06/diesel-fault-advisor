package com.truckadvisor.dieselfaultadvisor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for watsonx.ai integration
 * Values are loaded from application.properties or environment variables
 */
@Component
@ConfigurationProperties(prefix = "watsonx")
@Getter
@Setter
public class WatsonxConfig {
    
    /**
     * IBM Cloud API key for authentication
     * Should be set via environment variable: WATSONX_API_KEY
     */
    private String apiKey;
    
    /**
     * watsonx.ai project ID
     * Should be set via environment variable: WATSONX_PROJECT_ID
     */
    private String projectId;
    
    /**
     * watsonx.ai API endpoint URL
     * Default: https://us-south.ml.cloud.ibm.com
     */
    private String url = "https://us-south.ml.cloud.ibm.com";
    
    /**
     * Model ID to use for text generation
     * Default: ibm/granite-3-8b-instruct
     */
    private String modelId = "ibm/granite-3-8b-instruct";
    
    /**
     * Maximum number of tokens to generate
     * Default: 1000
     */
    private Integer maxTokens = 1000;
    
    /**
     * Temperature for text generation (0.0 - 2.0)
     * Lower values = more deterministic, Higher values = more creative
     * Default: 0.7
     */
    private Double temperature = 0.7;
    
    /**
     * Top-p sampling parameter
     * Default: 1.0
     */
    private Double topP = 1.0;
    
    /**
     * Top-k sampling parameter
     * Default: 50
     */
    private Integer topK = 50;
    
    /**
     * Repetition penalty
     * Default: 1.0
     */
    private Double repetitionPenalty = 1.0;
}

// Made with Bob
