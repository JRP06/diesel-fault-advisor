package com.truckadvisor.dieselfaultadvisor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for general truck assistant chat questions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    
    /**
     * The user's original question
     */
    private String question;
    
    /**
     * The AI assistant's answer
     */
    private String answer;
    
    /**
     * Timestamp when the response was generated
     */
    private String timestamp;
}

// Made with Bob