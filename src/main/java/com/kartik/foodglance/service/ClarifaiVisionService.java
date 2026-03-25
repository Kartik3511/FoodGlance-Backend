package com.kartik.foodglance.service;

import com.kartik.foodglance.model.VisionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ClarifaiVisionService {

    private static final Logger logger = LoggerFactory.getLogger(ClarifaiVisionService.class);
    
    private static final String CLARIFAI_API_URL = 
            "https://api.clarifai.com/v2/models/food-item-recognition/outputs";
    
    @Value("${clarifai.api-key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    // Indian food keywords and their Clarifai label mappings
    private static final Map<String, String[]> INDIAN_FOOD_MAPPINGS = Map.ofEntries(
        // Dal variations
        Map.entry("dal", new String[]{"lentil", "dal", "daal", "curry", "legume"}),
        Map.entry("lentils", new String[]{"lentil", "dal", "pulse", "legume"}),
        
        // Chapati/Roti variations
        Map.entry("chapati", new String[]{"flatbread", "roti", "chapati", "bread", "naan"}),
        Map.entry("roti", new String[]{"flatbread", "roti", "chapati", "bread"}),
        Map.entry("naan", new String[]{"naan", "flatbread", "bread"}),
        
        // Rice dishes
        Map.entry("biryani", new String[]{"biryani", "rice", "pilaf", "rice dish"}),
        Map.entry("pulao", new String[]{"pilaf", "pulao", "rice", "rice dish"}),
        Map.entry("rice", new String[]{"rice", "steamed rice", "white rice"}),
        
        // Paneer
        Map.entry("paneer", new String[]{"paneer", "cheese", "cottage cheese", "tofu"}),
        
        // Samosa
        Map.entry("samosa", new String[]{"samosa", "pastry", "dumpling", "fried"}),
        
        // Dosa/Idli
        Map.entry("dosa", new String[]{"dosa", "crepe", "pancake", "fermented"}),
        Map.entry("idli", new String[]{"idli", "steamed", "cake", "dumpling"}),
        
        // Vada
        Map.entry("vada", new String[]{"vada", "fritter", "fried", "donut", "pakora"}),
        
        // Poha
        Map.entry("poha", new String[]{"poha", "flattened rice", "rice flakes"}),
        
        // Upma
        Map.entry("upma", new String[]{"upma", "semolina", "porridge"}),
        
        // Paratha
        Map.entry("paratha", new String[]{"paratha", "flatbread", "fried bread"}),
        
        // Puri/Bhakri
        Map.entry("puri", new String[]{"puri", "poori", "fried bread", "flatbread"}),
        Map.entry("bhakri", new String[]{"bhakri", "flatbread", "millet bread"})
    );
    
    // Generic food category mappings
    private static final Map<String, String> CATEGORY_MAPPINGS = Map.ofEntries(
        Map.entry("curry", "dal"),
        Map.entry("stew", "dal"),
        Map.entry("flatbread", "chapati"),
        Map.entry("bread", "chapati"),
        Map.entry("rice dish", "rice"),
        Map.entry("fried rice", "biryani"),
        Map.entry("pancake", "dosa"),
        Map.entry("crepe", "dosa"),
        Map.entry("fritter", "vada"),
        Map.entry("dumpling", "samosa")
    );
    
    public ClarifaiVisionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Detect food from image using Clarifai Food Recognition API
     * @param imageBytes Raw image bytes (JPEG/PNG)
     * @return VisionResult with detected food label and confidence
     */
    public VisionResult detectFood(byte[] imageBytes) {
        try {
            logger.info("Calling Clarifai Food Recognition API");
            
            // Encode image to base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            // Build request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Key " + apiKey);
            
            // Clarifai request format
            Map<String, Object> requestBody = Map.of(
                "inputs", List.of(
                    Map.of(
                        "data", Map.of(
                            "image", Map.of(
                                "base64", base64Image
                            )
                        )
                    )
                )
            );
            
            HttpEntity<Map<String, Object>> requestEntity = 
                    new HttpEntity<>(requestBody, headers);
            
            // Call Clarifai API
            ResponseEntity<Map> response = restTemplate.exchange(
                CLARIFAI_API_URL,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            logger.info("Clarifai API response status: {}", response.getStatusCode());
            
            // Parse response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                return parseClarifaiResponse(responseBody);
            } else {
                logger.warn("Clarifai returned empty response");
                return new VisionResult("NOT_FOOD", 0);
            }
            
        } catch (RestClientException e) {
            logger.error("Error calling Clarifai API: {}", e.getMessage());
            throw new RuntimeException("Failed to detect food from image: " + e.getMessage());
        }
    }
    
    /**
     * Parse Clarifai response and extract best food match
     */
    @SuppressWarnings("unchecked")
    private VisionResult parseClarifaiResponse(Map<String, Object> response) {
        try {
            logger.debug("Parsing Clarifai response");
            
            // Navigate through Clarifai's nested response structure
            Map<String, Object> status = (Map<String, Object>) response.get("status");
            if (status != null) {
                Integer code = (Integer) status.get("code");
                if (code != null && code != 10000) {
                    logger.warn("Clarifai returned error status: {}", status.get("description"));
                    return new VisionResult("NOT_FOOD", 0);
                }
            }
            
            List<Map<String, Object>> outputs = (List<Map<String, Object>>) response.get("outputs");
            if (outputs == null || outputs.isEmpty()) {
                logger.warn("No outputs in Clarifai response");
                return new VisionResult("NOT_FOOD", 0);
            }
            
            Map<String, Object> output = outputs.get(0);
            Map<String, Object> data = (Map<String, Object>) output.get("data");
            if (data == null) {
                logger.warn("No data in Clarifai output");
                return new VisionResult("NOT_FOOD", 0);
            }
            
            List<Map<String, Object>> concepts = (List<Map<String, Object>>) data.get("concepts");
            if (concepts == null || concepts.isEmpty()) {
                logger.warn("No concepts detected by Clarifai");
                return new VisionResult("NOT_FOOD", 0);
            }
            
            // Log all detected concepts
            logger.info("Clarifai detected {} concepts:", concepts.size());
            for (int i = 0; i < Math.min(5, concepts.size()); i++) {
                Map<String, Object> concept = concepts.get(i);
                String name = (String) concept.get("name");
                Double value = (Double) concept.get("value");
                logger.info("  {}. {} (confidence: {})", i + 1, name, 
                    String.format("%.2f%%", value * 100));
            }
            
            // Find best matching Indian food
            VisionResult bestMatch = findBestIndianFoodMatch(concepts);
            
            if (bestMatch != null) {
                logger.info("Best match: {} ({}% confidence)", 
                    bestMatch.label, bestMatch.confidence);
                return bestMatch;
            }
            
            // If no Indian food match, use generic mapping
            Map<String, Object> topConcept = concepts.get(0);
            String topLabel = (String) topConcept.get("name");
            Double topConfidence = (Double) topConcept.get("value");
            
            String mappedFood = mapGenericCategory(topLabel);
            int confidence = (int) (topConfidence * 100);
            
            logger.info("Using generic mapping: {} → {}", topLabel, mappedFood);
            return new VisionResult(mappedFood, confidence);
            
        } catch (Exception e) {
            logger.error("Error parsing Clarifai response: {}", e.getMessage(), e);
            return new VisionResult("NOT_FOOD", 0);
        }
    }
    
    /**
     * Find best matching Indian food from Clarifai concepts
     */
    @SuppressWarnings("unchecked")
    private VisionResult findBestIndianFoodMatch(List<Map<String, Object>> concepts) {
        // Check each detected concept against Indian food mappings
        for (Map<String, Object> concept : concepts) {
            String label = ((String) concept.get("name")).toLowerCase();
            Double confidence = (Double) concept.get("value");
            
            // Check if this label matches any Indian food
            for (Map.Entry<String, String[]> entry : INDIAN_FOOD_MAPPINGS.entrySet()) {
                String foodName = entry.getKey();
                String[] keywords = entry.getValue();
                
                for (String keyword : keywords) {
                    if (label.contains(keyword.toLowerCase()) || 
                        keyword.toLowerCase().contains(label)) {
                        
                        int confidencePercent = (int) (confidence * 100);
                        logger.info("Matched '{}' to Indian food '{}'", label, foodName);
                        return new VisionResult(foodName, confidencePercent);
                    }
                }
            }
        }
        
        return null; // No Indian food match found
    }
    
    /**
     * Map generic food categories to specific foods
     */
    private String mapGenericCategory(String category) {
        String lower = category.toLowerCase().trim();
        
        // Check category mappings
        for (Map.Entry<String, String> entry : CATEGORY_MAPPINGS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Return as-is if no mapping
        return category;
    }
}
