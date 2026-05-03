package com.truckadvisor.dieselfaultadvisor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Detroit Series 60 DDEC fault code reference data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaultCodeData {
    private String spn;
    private String fmi;
    private String system;
    private String description;
}

// Made with Bob