package com.truckadvisor.dieselfaultadvisor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for fault code analysis
 * Contains SPN (Suspect Parameter Number) and FMI (Failure Mode Identifier)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaultCodeRequest {
    
    @NotBlank(message = "SPN (Suspect Parameter Number) is required")
    private String spn;
    
    @NotBlank(message = "FMI (Failure Mode Identifier) is required")
    private String fmi;
    
    private String language; // Optional: "en" or "es" for response language
}

// Made with Bob
