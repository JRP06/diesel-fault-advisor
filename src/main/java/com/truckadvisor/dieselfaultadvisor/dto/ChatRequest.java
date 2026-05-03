package com.truckadvisor.dieselfaultadvisor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for general truck assistant chat questions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {
    
    /**
     * The user's question in plain language
     */
    @NotBlank(message = "Question is required")
    private String question;
    
    /**
     * Language preference (en or es)
     */
    private String language;
}

// Made with Bob