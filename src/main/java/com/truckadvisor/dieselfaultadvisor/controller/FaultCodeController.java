package com.truckadvisor.dieselfaultadvisor.controller;

import com.truckadvisor.dieselfaultadvisor.dto.FaultCodeAnalysis;
import com.truckadvisor.dieselfaultadvisor.dto.FaultCodeRequest;
import com.truckadvisor.dieselfaultadvisor.service.WatsonxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for diesel truck fault code analysis
 * Provides endpoints to analyze fault codes using watsonx.ai
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure appropriately for production
public class FaultCodeController {
    
    private final WatsonxService watsonxService;
    
    /**
     * Analyze a diesel truck fault code
     * 
     * @param request Contains SPN and FMI numbers
     * @return Comprehensive fault code analysis with explanation, causes, fixes, tools, and parts
     */
    @PostMapping("/analyze")
    public ResponseEntity<FaultCodeAnalysis> analyzeFaultCode(@Valid @RequestBody FaultCodeRequest request) {
        log.info("Received fault code analysis request: SPN={}, FMI={}", request.getSpn(), request.getFmi());
        
        try {
            FaultCodeAnalysis analysis = watsonxService.analyzeFaultCode(
                request.getSpn(), 
                request.getFmi()
            );
            
            log.info("Successfully analyzed fault code: {}", analysis.getFaultCode());
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            log.error("Error analyzing fault code: SPN={}, FMI={}", request.getSpn(), request.getFmi(), e);
            throw e; // Will be handled by GlobalExceptionHandler
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Diesel Fault Advisor API is running");
    }
}

// Made with Bob
