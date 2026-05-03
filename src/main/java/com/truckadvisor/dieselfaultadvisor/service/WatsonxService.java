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
    private final FaultCodeDatasetService faultCodeDatasetService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Analyze a fault code using watsonx.ai Granite model
     * 
     * @param spn Suspect Parameter Number
     * @param fmi Failure Mode Identifier
     * @return Comprehensive fault code analysis
     */
    public FaultCodeAnalysis analyzeFaultCode(String spn, String fmi, String language) {
        log.info("=== Starting fault code analysis: SPN {} FMI {} Language {} ===", spn, fmi, language);
        
        try {
            // Build the prompt for the AI model
            String prompt = buildPrompt(spn, fmi, language);
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
     * Answer a follow-up question about a fault code
     *
     * @param spn Suspect Parameter Number
     * @param fmi Failure Mode Identifier
     * @param question The follow-up question
     * @param language Language preference
     * @return Answer to the follow-up question
     */
    public String answerFollowUpQuestion(String spn, String fmi, String question, String language) {
        log.info("=== Answering follow-up question for SPN {} FMI {} ===", spn, fmi);
        log.debug("Question: {}", question);
        
        try {
            // Build a prompt for the follow-up question
            String prompt = buildFollowUpPrompt(spn, fmi, question, language);
            
            // Create API request
            WatsonxApiRequest request = buildApiRequest(prompt);
            
            // Call watsonx.ai API
            WatsonxApiResponse response = callWatsonxApi(request);
            
            // Extract the answer
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                String answer = response.getResults().get(0).getGeneratedText();
                log.info("=== Successfully answered follow-up question ===");
                return answer.trim();
            } else {
                throw new WatsonxApiException("No answer generated for follow-up question");
            }
            
        } catch (Exception e) {
            log.error("=== FAILED to answer follow-up question ===", e);
            throw e;
        }
    }
    
    /**
     * Answer a general truck question (chat assistant)
     *
     * @param question The user's question in plain language
     * @param language Language preference
     * @return Answer to the question
     */
    public String answerChatQuestion(String question, String language) {
        log.info("=== Answering chat question ===");
        log.debug("Question: {}", question);
        
        try {
            // Build a prompt for the chat question
            String prompt = buildChatPrompt(question, language);
            
            // Create API request
            WatsonxApiRequest request = buildApiRequest(prompt);
            
            // Call watsonx.ai API
            WatsonxApiResponse response = callWatsonxApi(request);
            
            // Extract the answer
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                String answer = response.getResults().get(0).getGeneratedText();
                // Clean up the response: remove trailing numbers and formatting artifacts
                answer = cleanResponse(answer);
                log.info("=== Successfully answered chat question ===");
                return answer.trim();
            } else {
                throw new WatsonxApiException("No answer generated for chat question");
            }
            
        } catch (Exception e) {
            log.error("=== FAILED to answer chat question ===", e);
            throw e;
        }
    }
    
    /**
     * Clean up AI response by removing trailing numbers and formatting artifacts
     */
    private String cleanResponse(String text) {
        if (text == null) return "";
        
        // Remove trailing numbers at end of sentences (e.g., "sentence.3" or "sentence. 5")
        text = text.replaceAll("\\.\\s*\\d+\\s*$", ".");
        text = text.replaceAll("\\.\\s*\\d+\\s*\\.", ".");
        
        // Remove standalone numbers at the end
        text = text.replaceAll("\\s+\\d+\\s*$", "");
        
        // Remove multiple spaces
        text = text.replaceAll("\\s+", " ");
        
        return text.trim();
    }

    /**
     * Build a prompt for chat questions with strict guardrails
     */
    private String buildChatPrompt(String question, String language) {
        boolean isSpanish = "es".equalsIgnoreCase(language);
        
        if (isSpanish) {
            return String.format("""
                INSTRUCCIÓN ABSOLUTA: Eres un asistente SOLO para problemas mecánicos de camiones diesel.
                
                Si la pregunta NO es sobre motores diesel o mecánica de camiones, responde ÚNICAMENTE: "Solo puedo ayudar con problemas mecánicos de camiones diesel y códigos de falla." No agregues nada más.
                
                Si SÍ es sobre mecánica diesel, responde en máximo 3 oraciones simples.
                
                Pregunta: %s
                Respuesta:
                """, question);
        } else {
            return String.format("""
                ABSOLUTE INSTRUCTION: You are an assistant ONLY for diesel truck mechanical problems.
                
                If the question is NOT about diesel engines or truck mechanics, respond ONLY: "I can only help with diesel truck mechanical issues and fault codes." Add nothing else.
                
                If YES about diesel mechanics, answer in maximum 3 simple sentences.
                
                Question: %s
                Answer:
                """, question);
        }
    }
    
    
    
    /**
     * Build a prompt for follow-up questions
     */
    private String buildFollowUpPrompt(String spn, String fmi, String question, String language) {
        boolean isSpanish = "es".equalsIgnoreCase(language);
        
        if (isSpanish) {
            return String.format("""
                Eres un experto mecánico de camiones diesel respondiendo una pregunta de seguimiento sobre el código de falla SPN %s FMI %s.
                
                IMPORTANTE: Responde COMPLETAMENTE EN ESPAÑOL. Usa palabras simples y cotidianas.
                
                Pregunta del usuario: %s
                
                Proporciona una respuesta clara y detallada. Explica CADA término técnico entre paréntesis. Menciona ubicaciones físicas cuando sea relevante. El usuario NO es mecánico.
                """, spn, fmi, question);
        } else {
            return String.format("""
                You are an expert diesel truck mechanic answering a follow-up question about fault code SPN %s FMI %s.
                
                IMPORTANT: Use simple, everyday language. The user is NOT a mechanic.
                
                User's question: %s
                
                Provide a clear and detailed answer. Explain EVERY technical term in parentheses. Mention physical locations when relevant. Be helpful and thorough.
                """, spn, fmi, question);
        }
    }
    
    /**
     * Build a specialized prompt for diesel truck fault code analysis
     */
    private String buildPrompt(String spn, String fmi, String language) {
        boolean isSpanish = "es".equalsIgnoreCase(language);
        
        // Get Detroit Series 60 specific context
        String faultCodeContext = faultCodeDatasetService.getFaultCodeContext(spn, fmi);
        
        if (isSpanish) {
            return String.format("""
                Eres mecánico Detroit Series 60 DDEC. Código: SPN %s FMI %s
                
                CONTEXTO: %s
                
                IMPORTANTE: Responde EN ESPAÑOL. Máximo 3-4 oraciones por sección. Palabras simples. Directo al punto.
                
                EXPLICACIÓN:
                [2-3 oraciones. Qué significa. Explica términos técnicos entre paréntesis.]
                
                CAUSAS COMUNES:
                - [3-4 causas. Menciona ubicación física.]
                
                INSTRUCCIONES DE REPARACIÓN:
                1. [Pasos cortos y claros. Qué hacer.]
                2. [Máximo 4-5 pasos.]
                
                HERRAMIENTAS NECESARIAS:
                - [Lista simple de herramientas]
                
                PARTES A COMPRAR:
                - [Nombre parte + ubicación + número OEM si posible]
                """, spn, fmi, faultCodeContext);
        } else {
            return String.format("""
                Detroit Series 60 DDEC mechanic. Code: SPN %s FMI %s
                
                CONTEXT: %s
                
                IMPORTANT: Simple words. Max 3-4 sentences per section. Straight to the point.
                
                EXPLANATION:
                [2-3 sentences. What it means. Explain technical terms in parentheses.]
                
                ROOT CAUSES:
                - [3-4 causes. Mention physical location.]
                
                FIX INSTRUCTIONS:
                1. [Short clear steps. What to do.]
                2. [Max 4-5 steps.]
                
                TOOLS NEEDED:
                - [Simple tool list]
                
                PARTS TO BUY:
                - [Part name + location + OEM number if possible]
                """, spn, fmi, faultCodeContext);
        }
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
     * Supports both English and Spanish section markers
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
            // Detect language based on section markers
            boolean isSpanish = generatedText.contains("EXPLICACIÓN:") || generatedText.contains("CAUSAS COMUNES:");
            
            FaultCodeAnalysis analysis;
            if (isSpanish) {
                log.info("Detected Spanish response, using Spanish section markers");
                analysis = FaultCodeAnalysis.builder()
                    .faultCode(String.format("SPN %s FMI %s", spn, fmi))
                    .explanation(extractSection(generatedText, "EXPLICACIÓN:", "CAUSAS COMUNES:"))
                    .rootCauses(extractList(generatedText, "CAUSAS COMUNES:", "INSTRUCCIONES DE REPARACIÓN:"))
                    .fixInstructions(extractNumberedList(generatedText, "INSTRUCCIONES DE REPARACIÓN:", "HERRAMIENTAS NECESARIAS:"))
                    .toolsNeeded(extractList(generatedText, "HERRAMIENTAS NECESARIAS:", "PARTES A COMPRAR:"))
                    .partsToBuy(extractList(generatedText, "PARTES A COMPRAR:", null))
                    .timestamp(Instant.now().toString())
                    .build();
            } else {
                log.info("Detected English response, using English section markers");
                analysis = FaultCodeAnalysis.builder()
                    .faultCode(String.format("SPN %s FMI %s", spn, fmi))
                    .explanation(extractSection(generatedText, "EXPLANATION:", "ROOT CAUSES:"))
                    .rootCauses(extractList(generatedText, "ROOT CAUSES:", "FIX INSTRUCTIONS:"))
                    .fixInstructions(extractNumberedList(generatedText, "FIX INSTRUCTIONS:", "TOOLS NEEDED:"))
                    .toolsNeeded(extractList(generatedText, "TOOLS NEEDED:", "PARTS TO BUY:"))
                    .partsToBuy(extractList(generatedText, "PARTS TO BUY:", null))
                    .timestamp(Instant.now().toString())
                    .build();
            }
            
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
