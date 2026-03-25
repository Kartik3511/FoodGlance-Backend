package com.kartik.foodglance.service;

import com.kartik.foodglance.model.VisionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SpoonacularVisionService {

    private static final Logger logger = LoggerFactory.getLogger(SpoonacularVisionService.class);
    
    private static final String SPOONACULAR_API_URL = 
            "https://api.spoonacular.com/food/images/classify";
    
    @Value("${spoonacular.api-key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    // Indian food keywords to map Spoonacular results
    private static final Set<String> INDIAN_FOOD_KEYWORDS = Set.of(
        "dal", "chapati", "paratha", "biryani", "samosa", "paneer",
        "bhakri", "puri", "dosa", "idli", "vada", "upma", "poha",
        "curry", "rice", "roti", "naan", "kulcha", "tikka", "tandoori",
        "butter chicken", "palak", "chole", "aloo", "gobi", "masala"
    );
    
    public SpoonacularVisionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Detect food from image using Spoonacular Image Classification API
     * @param imageBytes Raw image bytes (JPEG/PNG)
     * @return VisionResult with detected food label and confidence
     */
    public VisionResult detectFood(byte[] imageBytes) {
        try {
            logger.info("Calling Spoonacular Image Classification API");
            
            // Build multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Add image file to request
            ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "image.jpg";
                }
            };
            body.add("file", fileResource);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                    new HttpEntity<>(body, headers);
            
            String url = SPOONACULAR_API_URL + "?apiKey=" + apiKey;
            
            // Call Spoonacular API
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            logger.info("Spoonacular API response status: {}", response.getStatusCode());
            
            // Parse response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                return parseSpoonacularResponse(responseBody);
            } else {
                logger.warn("Spoonacular returned empty response");
                return new VisionResult("NOT_FOOD", 0);
            }
            
        } catch (RestClientException e) {
            logger.error("Error calling Spoonacular API: {}", e.getMessage());
            throw new RuntimeException("Failed to detect food from image: " + e.getMessage());
        }
    }
    
    /**
     * Parse Spoonacular classification response to VisionResult format
     */
    private VisionResult parseSpoonacularResponse(Map<String, Object> response) {
        logger.debug("Parsing Spoonacular response: {}", response);
        
        // Spoonacular returns: { "category": "dish", "probability": 0.95 }
        // OR { "status": "failure", "message": "..." }
        
        if ("failure".equals(response.get("status"))) {
            logger.warn("Spoonacular classification failed: {}", response.get("message"));
            return new VisionResult("NOT_FOOD", 0);
        }
        
        // Extract category (food type)
        String category = (String) response.get("category");
        Object probabilityObj = response.get("probability");
        
        int confidence = 85; // Default confidence
        if (probabilityObj instanceof Number) {
            double probability = ((Number) probabilityObj).doubleValue();
            confidence = (int) (probability * 100);
        }
        
        if (category != null && !category.isEmpty()) {
            logger.info("Spoonacular detected category: {} (confidence: {}%)", category, confidence);
            
            // Normalize food name
            String normalizedFood = normalizeFoodName(category);
            logger.info("Normalized to: {}", normalizedFood);
            
            return new VisionResult(normalizedFood, confidence);
        } else {
            logger.warn("No category found in Spoonacular response");
            return new VisionResult("NOT_FOOD", 0);
        }
    }
    
    /**
     * Normalize food names to match our database
     * Maps generic categories to specific foods when possible
     */
    private String normalizeFoodName(String category) {
        String lower = category.toLowerCase().trim();
        
        // Direct match for Indian foods
        for (String keyword : INDIAN_FOOD_KEYWORDS) {
            if (lower.contains(keyword)) {
                return keyword;
            }
        }
        
        // Map common Spoonacular categories to food names
        switch (lower) {
            case "bread":
            case "flatbread":
                return "chapati"; // Default Indian flatbread
            case "rice dish":
            case "rice":
                return "rice";
            case "curry":
            case "stew":
                return "dal"; // Default Indian curry
            case "pancake":
            case "crepe":
                return "dosa"; // Indian pancake
            case "fritter":
            case "fried food":
                return "vada"; // Indian fritter
            case "dumpling":
            case "pastry":
                return "samosa"; // Indian dumpling
            default:
                // Return as-is if no mapping
                return category;
        }
    }
}
