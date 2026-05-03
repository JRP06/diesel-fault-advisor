package com.truckadvisor.dieselfaultadvisor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for follow-up questions about a fault code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpRequest {
    
    /**
     * Original SPN number
     */
    @NotBlank(message = "SPN is required")
    private String spn;
    
    /**
     * Original FMI number
     */
    @NotBlank(message = "FMI is required")
    private String fmi;
    
    /**
     * The follow-up question to ask
     */
    @NotBlank(message = "Question is required")
    private String question;
    
    /**
     * Language preference (en or es)
     */
    private String language;
}

// Made with Bob