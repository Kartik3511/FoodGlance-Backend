package com.kartik.foodglance.service;

import com.kartik.foodglance.model.NutritionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Hybrid Nutrition Service - Orchestrates ICMR and USDA data sources.
 * 
 * Strategy: ICMR-first with USDA fallback
 * 1. Try ICMR-NIN IFCT 2017 database first (for Indian foods)
 * 2. If not found, fallback to USDA FoodData Central API (for international foods)
 * 3. Return NutritionData with source indicator ("ICMR" or "USDA")
 */
@Service
public class HybridNutritionService {

    private static final Logger logger = LoggerFactory.getLogger(HybridNutritionService.class);

    private final IcmrNutritionService icmrService;
    private final NutritionService usdaService;

    public HybridNutritionService(IcmrNutritionService icmrService, NutritionService usdaService) {
        this.icmrService = icmrService;
        this.usdaService = usdaService;
    }

    /**
     * Get nutrition data using hybrid ICMR + USDA approach.
     * 
     * @param foodName The food name to search for
     * @return NutritionData with source field set to "ICMR" or "USDA", or fallback (0s) if not found
     */
    public NutritionData getNutrition(String foodName) {
        if (foodName == null || foodName.isBlank()) {
            logger.warn("Empty food name provided to hybrid service");
            return createFallback();
        }

        long startTime = System.currentTimeMillis();
        logger.info("Hybrid lookup for: {}", foodName);

        // Step 1: Try ICMR first
        try {
            NutritionData icmrData = icmrService.searchIcmrFood(foodName);
            if (icmrData != null) {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✓ Found in ICMR: {} ({}ms)", foodName, duration);
                return icmrData;
            }
        } catch (Exception e) {
            logger.error("Error searching ICMR for {}: {}", foodName, e.getMessage());
        }

        // Step 2: Fallback to USDA
        try {
            NutritionData usdaData = usdaService.getNutrition(foodName);
            if (usdaData != null && !isEmptyNutrition(usdaData)) {
                usdaData.setSource("USDA");
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✓ Found in USDA: {} ({}ms)", foodName, duration);
                return usdaData;
            }
        } catch (Exception e) {
            logger.error("Error searching USDA for {}: {}", foodName, e.getMessage());
        }

        // Step 3: Both failed - return fallback
        logger.warn("✗ Not found in ICMR or USDA: {}", foodName);
        return createFallback();
    }

    /**
     * Search for multiple food suggestions (for autocomplete/search).
     * Currently delegates to USDA since ICMR dataset is curated (26 foods).
     * 
     * TODO: Implement ICMR search first, then augment with USDA results.
     */
    public java.util.List<com.kartik.foodglance.model.FoodSearchResult> searchFoods(String query) {
        // For now, use USDA search since ICMR is a small curated dataset
        // In the future, this could search ICMR first and merge results
        return usdaService.searchFoods(query);
    }

    /**
     * Check if nutrition data is empty (all zeros).
     */
    private boolean isEmptyNutrition(NutritionData data) {
        return "0".equals(data.getCalories()) &&
               "0".equals(data.getProtein()) &&
               "0".equals(data.getCarbs()) &&
               "0".equals(data.getFat());
    }

    /**
     * Create fallback nutrition data (all zeros, no source).
     */
    private NutritionData createFallback() {
        return new NutritionData("0", "0", "0", "0", "UNKNOWN");
    }

    /**
     * Get service statistics.
     */
    public String getStats() {
        int icmrCount = icmrService.getTotalFoods();
        return String.format("Hybrid Service: ICMR=%d foods, USDA=API", icmrCount);
    }
}
