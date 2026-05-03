package com.truckadvisor.dieselfaultadvisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truckadvisor.dieselfaultadvisor.dto.FaultCodeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service to load and query Detroit Series 60 DDEC fault code dataset
 */
@Service
@Slf4j
public class FaultCodeDatasetService {
    
    private List<FaultCodeData> faultCodes = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void loadDataset() {
        try {
            ClassPathResource resource = new ClassPathResource("detroit-series60-fault-codes.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            JsonNode faultCodesNode = root.get("faultCodes");
            
            if (faultCodesNode != null && faultCodesNode.isArray()) {
                for (JsonNode node : faultCodesNode) {
                    FaultCodeData data = new FaultCodeData(
                        node.get("spn").asText(),
                        node.get("fmi").asText(),
                        node.get("system").asText(),
                        node.get("description").asText()
                    );
                    faultCodes.add(data);
                }
            }
            
            log.info("Loaded {} Detroit Series 60 DDEC fault codes", faultCodes.size());
            
        } catch (IOException e) {
            log.error("Failed to load fault code dataset", e);
        }
    }
    
    /**
     * Find fault code data by SPN and FMI
     */
    public Optional<FaultCodeData> findFaultCode(String spn, String fmi) {
        return faultCodes.stream()
            .filter(fc -> fc.getSpn().equals(spn) && fc.getFmi().equals(fmi))
            .findFirst();
    }
    
    /**
     * Get context string for AI prompt with relevant fault codes
     */
    public String getFaultCodeContext(String spn, String fmi) {
        Optional<FaultCodeData> exactMatch = findFaultCode(spn, fmi);
        
        StringBuilder context = new StringBuilder();
        context.append("Detroit Series 60 DDEC Fault Code Reference:\n\n");
        
        if (exactMatch.isPresent()) {
            FaultCodeData data = exactMatch.get();
            context.append(String.format("EXACT MATCH - SPN %s FMI %s:\n", spn, fmi));
            context.append(String.format("System: %s\n", data.getSystem()));
            context.append(String.format("Description: %s\n\n", data.getDescription()));
        }
        
        // Add related codes with same SPN
        context.append("Related codes with same SPN:\n");
        faultCodes.stream()
            .filter(fc -> fc.getSpn().equals(spn) && !fc.getFmi().equals(fmi))
            .limit(3)
            .forEach(fc -> context.append(String.format("- SPN %s FMI %s: %s - %s\n", 
                fc.getSpn(), fc.getFmi(), fc.getSystem(), fc.getDescription())));
        
        return context.toString();
    }
    
    /**
     * Get all fault codes for general context
     */
    public String getAllFaultCodesContext() {
        StringBuilder context = new StringBuilder();
        context.append("Detroit Series 60 DDEC Common Fault Codes:\n\n");
        
        faultCodes.stream()
            .limit(20) // Include top 20 for general context
            .forEach(fc -> context.append(String.format("SPN %s FMI %s: %s - %s\n",
                fc.getSpn(), fc.getFmi(), fc.getSystem(), fc.getDescription())));
        
        return context.toString();
    }
}

// Made with Bob