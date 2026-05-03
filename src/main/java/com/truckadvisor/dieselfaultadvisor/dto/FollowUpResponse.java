package com.truckadvisor.dieselfaultadvisor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for follow-up questions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpResponse {
    
    /**
     * The original fault code
     */
    private String faultCode;
    
    /**
     * The follow-up question that was asked
     */
    private String question;
    
    /**
     * The AI's answer to the follow-up question
     */
    private String answer;
    
    /**
     * Timestamp when the response was generated
     */
    private String timestamp;
}

// Made with Bob