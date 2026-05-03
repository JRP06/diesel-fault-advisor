package com.truckadvisor.dieselfaultadvisor.controller;

import com.truckadvisor.dieselfaultadvisor.dto.ChatRequest;
import com.truckadvisor.dieselfaultadvisor.dto.ChatResponse;
import com.truckadvisor.dieselfaultadvisor.dto.FaultCodeAnalysis;
import com.truckadvisor.dieselfaultadvisor.dto.FaultCodeRequest;
import com.truckadvisor.dieselfaultadvisor.dto.FollowUpRequest;
import com.truckadvisor.dieselfaultadvisor.dto.FollowUpResponse;
import com.truckadvisor.dieselfaultadvisor.service.WatsonxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

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
        log.info("Received fault code analysis request: SPN={}, FMI={}, Language={}",
            request.getSpn(), request.getFmi(), request.getLanguage());
        
        try {
            // Default to English if language not specified
            String language = request.getLanguage() != null ? request.getLanguage() : "en";
            
            FaultCodeAnalysis analysis = watsonxService.analyzeFaultCode(
                request.getSpn(),
                request.getFmi(),
                language
            );
            
            log.info("Successfully analyzed fault code: {}", analysis.getFaultCode());
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            log.error("Error analyzing fault code: SPN={}, FMI={}", request.getSpn(), request.getFmi(), e);
            throw e; // Will be handled by GlobalExceptionHandler
        }
    }
    
    /**
     * Answer a follow-up question about a fault code
     *
     * @param request Contains SPN, FMI, question, and language
     * @return Answer to the follow-up question
     */
    @PostMapping("/followup")
    public ResponseEntity<FollowUpResponse> answerFollowUpQuestion(@Valid @RequestBody FollowUpRequest request) {
        log.info("Received follow-up question: SPN={}, FMI={}, Language={}",
            request.getSpn(), request.getFmi(), request.getLanguage());
        log.debug("Question: {}", request.getQuestion());
        
        try {
            // Default to English if language not specified
            String language = request.getLanguage() != null ? request.getLanguage() : "en";
            
            String answer = watsonxService.answerFollowUpQuestion(
                request.getSpn(),
                request.getFmi(),
                request.getQuestion(),
                language
            );
            
            FollowUpResponse response = FollowUpResponse.builder()
                .faultCode(String.format("SPN %s FMI %s", request.getSpn(), request.getFmi()))
                .question(request.getQuestion())
                .answer(answer)
                .timestamp(Instant.now().toString())
                .build();
            
            log.info("Successfully answered follow-up question");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error answering follow-up question: SPN={}, FMI={}",
                request.getSpn(), request.getFmi(), e);
            throw e; // Will be handled by GlobalExceptionHandler
        }
    }
    
    /**
     * Chat with the truck assistant for general questions
     *
     * @param request Contains question and language
     * @return AI assistant's answer
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat question, Language={}", request.getLanguage());
        log.debug("Question: {}", request.getQuestion());
        
        try {
            // Default to English if language not specified
            String language = request.getLanguage() != null ? request.getLanguage() : "en";
            
            String answer = watsonxService.answerChatQuestion(
                request.getQuestion(),
                language
            );
            
            ChatResponse response = ChatResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .timestamp(Instant.now().toString())
                .build();
            
            log.info("Successfully answered chat question");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error answering chat question", e);
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
