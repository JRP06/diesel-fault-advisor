package com.truckadvisor.dieselfaultadvisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truckadvisor.dieselfaultadvisor.config.WatsonxConfig;
import com.truckadvisor.dieselfaultadvisor.dto.FaultCodeAnalysis;
import com.truckadvisor.dieselfaultadvisor.dto.WatsonxApiRequest;
import com.truckadvisor.dieselfaultadvisor.dto.WatsonxApiResponse;
import com.truckadvisor.dieselfaultadvisor.exception.WatsonxApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for interacting with watsonx.ai API
 * Handles fault code analysis using IBM Granite model
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatsonxService {
    
    private final WatsonxConfig config;
    private final IamTokenService iamTokenService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Analyze a fault code using watsonx.ai Granite model
     * 
     * @param spn Suspect Parameter Number
     * @param fmi Failure Mode Identifier
     * @return Comprehensive fault code analysis
     */
    public FaultCodeAnalysis analyzeFaultCode(String spn, String fmi) {
        log.info("=== Starting fault code analysis: SPN {} FMI {} ===", spn, fmi);
        
        try {
            // Build the prompt for the AI model
            String prompt = buildPrompt(spn, fmi);
            log.debug("Built prompt with {} characters", prompt.length());
            
            // Create API request
            WatsonxApiRequest request = buildApiRequest(prompt);
            log.info("Created API request - Model: {}, Project: {}, Max Tokens: {}",
                config.getModelId(), config.getProjectId(), config.getMaxTokens());
            
            // Call watsonx.ai API
            WatsonxApiResponse response = callWatsonxApi(request);
            
            // Parse and structure the response
            FaultCodeAnalysis analysis = parseResponse(spn, fmi, response);
            log.info("=== Successfully completed fault code analysis ===");
            return analysis;
            
        } catch (Exception e) {
            log.error("=== FAILED to analyze fault code: SPN {} FMI {} ===", spn, fmi, e);
            throw e;
        }
    }
    
    /**
     * Build a specialized prompt for diesel truck fault code analysis
     */
    private String buildPrompt(String spn, String fmi) {
        return String.format("""
            You are an expert diesel truck mechanic and diagnostic specialist. Analyze the following fault code:
            
            Fault Code: SPN %s FMI %s
            
            Provide a comprehensive analysis in the following format:
            
            EXPLANATION:
            [Provide a clear, plain English explanation of what this fault code means]
            
            ROOT CAUSES:
            - [List 3-5 common root causes, one per line]
            
            FIX INSTRUCTIONS:
            1. [Step-by-step diagnostic and repair instructions]
            2. [Continue numbering each step]
            
            TOOLS NEEDED:
            - [List all tools required]
            
            PARTS TO BUY:
            - [List parts that may need replacement with OEM recommendations]
            
            Be specific, technical, and practical. Focus on actionable information for a mechanic.
            """, spn, fmi);
    }
    
    /**
     * Build the watsonx.ai API request
     */
    private WatsonxApiRequest buildApiRequest(String prompt) {
        return WatsonxApiRequest.builder()
            .modelId(config.getModelId())
            .input(prompt)
            .projectId(config.getProjectId())
            .parameters(WatsonxApiRequest.Parameters.builder()
                .decodingMethod("greedy")
                .maxNewTokens(config.getMaxTokens())
                .minNewTokens(50)
                .temperature(config.getTemperature())
                .topK(config.getTopK())
                .topP(config.getTopP())
                .repetitionPenalty(config.getRepetitionPenalty())
                .build())
            .build();
    }
    
    /**
     * Call the watsonx.ai API
     */
    private WatsonxApiResponse callWatsonxApi(WatsonxApiRequest request) {
        String url = null;
        try {
            url = config.getUrl() + "/ml/v1/text/generation?version=2023-05-29";
            log.info("Preparing watsonx.ai API call to: {}", url);
            
            // Get IAM token for authentication
            log.debug("Obtaining IAM token...");
            String iamToken = iamTokenService.getAccessToken();
            log.info("IAM token obtained successfully (length: {})", iamToken != null ? iamToken.length() : 0);
            
            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(iamToken);
            log.debug("Request headers configured");
            
            // Serialize request for logging
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.debug("Request payload: {}", requestJson);
            } catch (Exception jsonEx) {
                log.warn("Could not serialize request for logging", jsonEx);
            }
            
            HttpEntity<WatsonxApiRequest> entity = new HttpEntity<>(request, headers);
            
            log.info("Sending POST request to watsonx.ai API...");
            
            // First get the raw response as String to log it
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            log.info("Received response - Status: {}", rawResponse.getStatusCode());
            log.info("=== RAW JSON RESPONSE FROM WATSONX.AI ===");
            log.info("{}", rawResponse.getBody());
            log.info("=== END RAW JSON RESPONSE ===");
            
            // Now parse it into our DTO
            WatsonxApiResponse response;
            try {
                response = objectMapper.readValue(rawResponse.getBody(), WatsonxApiResponse.class);
                log.info("Successfully parsed response into WatsonxApiResponse DTO");
            } catch (Exception parseEx) {
                log.error("Failed to parse response into WatsonxApiResponse", parseEx);
                throw new WatsonxApiException("Failed to parse watsonx.ai response: " + parseEx.getMessage(), parseEx);
            }
            
            // Wrap in ResponseEntity for compatibility with existing code
            ResponseEntity<WatsonxApiResponse> typedResponse = ResponseEntity
                .status(rawResponse.getStatusCode())
                .body(response);
            
            log.info("Response has body: {}", typedResponse.getBody() != null);
            
            if (typedResponse.getStatusCode() == HttpStatus.OK && typedResponse.getBody() != null) {
                WatsonxApiResponse body = typedResponse.getBody();
                log.info("Response details - Model: {}, Results count: {}",
                    body.getModelId(),
                    body.getResults() != null ? body.getResults().size() : 0);
                
                if (body.getResults() != null && !body.getResults().isEmpty()) {
                    WatsonxApiResponse.Result firstResult = body.getResults().get(0);
                    log.info("First result - Generated tokens: {}, Input tokens: {}, Stop reason: {}",
                        firstResult.getGeneratedTokenCount(),
                        firstResult.getInputTokenCount(),
                        firstResult.getStopReason());
                    log.debug("Generated text preview: {}",
                        firstResult.getGeneratedText() != null && firstResult.getGeneratedText().length() > 100
                            ? firstResult.getGeneratedText().substring(0, 100) + "..."
                            : firstResult.getGeneratedText());
                }
                
                return body;
            } else {
                String errorMsg = String.format("Unexpected response from watsonx.ai API - Status: %s",
                    typedResponse.getStatusCode());
                log.error(errorMsg);
                throw new WatsonxApiException(errorMsg);
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP Client Error calling watsonx.ai API");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Status Text: {}", e.getStatusText());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("Request URL: {}", url);
            throw new WatsonxApiException(
                String.format("HTTP %s error from watsonx.ai API: %s - %s",
                    e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString()), e);
                    
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("HTTP Server Error from watsonx.ai API");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Status Text: {}", e.getStatusText());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("Request URL: {}", url);
            throw new WatsonxApiException(
                String.format("HTTP %s error from watsonx.ai API: %s - %s",
                    e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString()), e);
                    
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Network/Connection error calling watsonx.ai API");
            log.error("Request URL: {}", url);
            log.error("Error details: {}", e.getMessage(), e);
            throw new WatsonxApiException(
                "Network error connecting to watsonx.ai API: " + e.getMessage(), e);
                
        } catch (WatsonxApiException e) {
            // Re-throw our custom exceptions
            throw e;
            
        } catch (Exception e) {
            log.error("Unexpected error calling watsonx.ai API");
            log.error("Request URL: {}", url);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage(), e);
            throw new WatsonxApiException(
                "Unexpected error calling watsonx.ai API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse the AI response and structure it into FaultCodeAnalysis
     */
    private FaultCodeAnalysis parseResponse(String spn, String fmi, WatsonxApiResponse response) {
        log.info("Parsing watsonx.ai response...");
        
        if (response.getResults() == null || response.getResults().isEmpty()) {
            log.error("No results in watsonx.ai response");
            throw new WatsonxApiException("No results returned from watsonx.ai API");
        }
        
        String generatedText = response.getResults().get(0).getGeneratedText();
        log.info("Generated text length: {} characters", generatedText != null ? generatedText.length() : 0);
        log.debug("Full generated text:\n{}", generatedText);
        
        try {
            FaultCodeAnalysis analysis = FaultCodeAnalysis.builder()
                .faultCode(String.format("SPN %s FMI %s", spn, fmi))
                .explanation(extractSection(generatedText, "EXPLANATION:", "ROOT CAUSES:"))
                .rootCauses(extractList(generatedText, "ROOT CAUSES:", "FIX INSTRUCTIONS:"))
                .fixInstructions(extractNumberedList(generatedText, "FIX INSTRUCTIONS:", "TOOLS NEEDED:"))
                .toolsNeeded(extractList(generatedText, "TOOLS NEEDED:", "PARTS TO BUY:"))
                .partsToBuy(extractList(generatedText, "PARTS TO BUY:", null))
                .timestamp(Instant.now().toString())
                .build();
            
            log.info("Successfully parsed response into FaultCodeAnalysis");
            log.debug("Parsed sections - Explanation length: {}, Root causes: {}, Fix instructions: {}, Tools: {}, Parts: {}",
                analysis.getExplanation() != null ? analysis.getExplanation().length() : 0,
                analysis.getRootCauses() != null ? analysis.getRootCauses().size() : 0,
                analysis.getFixInstructions() != null ? analysis.getFixInstructions().size() : 0,
                analysis.getToolsNeeded() != null ? analysis.getToolsNeeded().size() : 0,
                analysis.getPartsToBuy() != null ? analysis.getPartsToBuy().size() : 0);
            
            return analysis;
            
        } catch (Exception e) {
            log.error("Error parsing watsonx.ai response", e);
            throw new WatsonxApiException("Failed to parse watsonx.ai response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract a section of text between two markers
     */
    private String extractSection(String text, String startMarker, String endMarker) {
        try {
            int start = text.indexOf(startMarker);
            if (start == -1) return "Information not available";
            
            start += startMarker.length();
            int end = endMarker != null ? text.indexOf(endMarker, start) : text.length();
            if (end == -1) end = text.length();
            
            return text.substring(start, end).trim();
        } catch (Exception e) {
            log.warn("Error extracting section between {} and {}", startMarker, endMarker, e);
            return "Information not available";
        }
    }
    
    /**
     * Extract a bulleted list from text
     */
    private List<String> extractList(String text, String startMarker, String endMarker) {
        String section = extractSection(text, startMarker, endMarker);
        List<String> items = new ArrayList<>();
        
        String[] lines = section.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("•")) {
                items.add(line.substring(1).trim());
            } else if (!line.isEmpty() && !line.equals(startMarker.trim())) {
                items.add(line);
            }
        }
        
        return items.isEmpty() ? List.of("Information not available") : items;
    }
    
    /**
     * Extract a numbered list from text
     */
    private List<String> extractNumberedList(String text, String startMarker, String endMarker) {
        String section = extractSection(text, startMarker, endMarker);
        List<String> items = new ArrayList<>();
        
        String[] lines = section.split("\n");
        Pattern numberPattern = Pattern.compile("^\\d+\\.\\s*(.+)$");
        
        for (String line : lines) {
            line = line.trim();
            Matcher matcher = numberPattern.matcher(line);
            if (matcher.matches()) {
                items.add(matcher.group(1).trim());
            } else if (!line.isEmpty() && !line.matches("^\\d+\\.$")) {
                items.add(line);
            }
        }
        
        return items.isEmpty() ? List.of("Information not available") : items;
    }
}

// Made with Bob
