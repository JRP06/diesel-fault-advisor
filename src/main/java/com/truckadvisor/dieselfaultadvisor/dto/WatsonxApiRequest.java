package com.truckadvisor.dieselfaultadvisor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for watsonx.ai API
 * Follows IBM watsonx.ai REST API specification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatsonxApiRequest {
    
    @JsonProperty("model_id")
    private String modelId;
    
    private String input;
    
    private Parameters parameters;
    
    @JsonProperty("project_id")
    private String projectId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Parameters {
        
        @JsonProperty("decoding_method")
        private String decodingMethod;
        
        @JsonProperty("max_new_tokens")
        private Integer maxNewTokens;
        
        @JsonProperty("min_new_tokens")
        private Integer minNewTokens;
        
        private Double temperature;
        
        @JsonProperty("top_k")
        private Integer topK;
        
        @JsonProperty("top_p")
        private Double topP;
        
        @JsonProperty("repetition_penalty")
        private Double repetitionPenalty;
        
        @JsonProperty("stop_sequences")
        private List<String> stopSequences;
    }
}

// Made with Bob
