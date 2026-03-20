package com.kartik.foodglance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartik.foodglance.model.IcmrFood;
import com.kartik.foodglance.model.IcmrFoodDatabase;
import com.kartik.foodglance.model.NutritionData;
import com.kartik.foodglance.util.FoodNameMatcher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Service for accessing ICMR-NIN IFCT 2017 food composition data.
 * Loads JSON data at startup and provides fast in-memory lookups with fuzzy matching.
 */
@Service
public class IcmrNutritionService {

    private static final Logger logger = LoggerFactory.getLogger(IcmrNutritionService.class);
    private static final String IFCT_JSON_PATH = "data/ifct-2017.json";

    private final FoodNameMatcher foodNameMatcher;
    private final ObjectMapper objectMapper;

    // In-memory storage for fast lookups
    private Map<String, IcmrFood> foodByIdMap;           // Key: food code (e.g., "A013")
    private Map<String, IcmrFood> foodByNameMap;         // Key: normalized name
    private List<IcmrFood> allFoods;                     // All foods for fuzzy search

    public IcmrNutritionService(FoodNameMatcher foodNameMatcher) {
        this.foodNameMatcher = foodNameMatcher;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Load IFCT JSON data at application startup.
     */
    @PostConstruct
    public void loadIfctData() {
        logger.info("Loading ICMR-NIN IFCT 2017 data from {}", IFCT_JSON_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(IFCT_JSON_PATH);
            InputStream inputStream = resource.getInputStream();

            IcmrFoodDatabase database = objectMapper.readValue(inputStream, IcmrFoodDatabase.class);

            allFoods = database.getFoods();
            foodByIdMap = new HashMap<>();
            foodByNameMap = new HashMap<>();

            // Build lookup maps
            for (IcmrFood food : allFoods) {
                // Index by food code
                foodByIdMap.put(food.getId(), food);

                // Index by normalized main name
                String normalizedName = foodNameMatcher.normalizeIndianFoodName(food.getName());
                foodByNameMap.put(normalizedName, food);

                // Index by all name variations
                if (food.getNames() != null) {
                    for (String variation : food.getNames()) {
                        String normalizedVariation = foodNameMatcher.normalizeIndianFoodName(variation);
                        foodByNameMap.putIfAbsent(normalizedVariation, food);
                    }
                }
            }

            logger.info("✓ Loaded {} foods from ICMR database", allFoods.size());
            logger.info("  - Metadata: {}", database.getMetadata().getSource());
            logger.info("  - Version: {}", database.getMetadata().getVersion());

        } catch (IOException e) {
            logger.error("Failed to load IFCT data from {}: {}", IFCT_JSON_PATH, e.getMessage());
            // Initialize empty maps to avoid null pointer exceptions
            allFoods = new ArrayList<>();
            foodByIdMap = new HashMap<>();
            foodByNameMap = new HashMap<>();
        }
    }

    /**
     * Search for food in ICMR database.
     * Returns NutritionData if found, null if not found.
     * 
     * Search strategy:
     * 1. Exact match (normalized)
     * 2. Fuzzy match with all food names
     * 3. Return best match above similarity threshold
     */
    @Cacheable(value = "icmr-nutrition", unless = "#result == null")
    public NutritionData searchIcmrFood(String foodName) {
        if (foodName == null || foodName.isBlank()) {
            return null;
        }

        logger.debug("Searching ICMR for: {}", foodName);

        // Step 1: Try exact match after normalization
        String normalized = foodNameMatcher.normalizeIndianFoodName(foodName);
        IcmrFood exactMatch = foodByNameMap.get(normalized);

        if (exactMatch != null) {
            logger.debug("✓ Exact ICMR match found: {}", exactMatch.getName());
            return convertToNutritionData(exactMatch);
        }

        // Step 2: Fuzzy search across all foods
        IcmrFood bestMatch = null;
        double bestScore = 0.0;

        for (IcmrFood food : allFoods) {
            // Check main name
            double scoreMain = foodNameMatcher.getMatchScore(foodName, food.getName());
            if (scoreMain > bestScore) {
                bestScore = scoreMain;
                bestMatch = food;
            }

            // Check all variations
            if (food.getNames() != null) {
                for (String variation : food.getNames()) {
                    double scoreVariation = foodNameMatcher.getMatchScore(foodName, variation);
                    if (scoreVariation > bestScore) {
                        bestScore = scoreVariation;
                        bestMatch = food;
                    }
                }
            }
        }

        // Return best match if above threshold (0.7 = 70% similarity)
        if (bestMatch != null && bestScore >= 0.7) {
            logger.debug("✓ Fuzzy ICMR match found: {} (score: {})", bestMatch.getName(), bestScore);
            return convertToNutritionData(bestMatch);
        }

        logger.debug("✗ No ICMR match found for: {}", foodName);
        return null;
    }

    /**
     * Convert IcmrFood to NutritionData format (compatible with existing system).
     */
    private NutritionData convertToNutritionData(IcmrFood food) {
        String calories = String.valueOf(Math.round(food.getEnergy()));
        String protein = String.valueOf(Math.round(food.getProtein()));
        String carbs = String.valueOf(Math.round(food.getCarbohydrate()));
        String fat = String.valueOf(Math.round(food.getFat()));

        return new NutritionData(calories, protein, carbs, fat, "ICMR");
    }

    /**
     * Get all available food categories.
     */
    public Set<String> getCategories() {
        Set<String> categories = new HashSet<>();
        for (IcmrFood food : allFoods) {
            categories.add(food.getCategory());
        }
        return categories;
    }

    /**
     * Get total number of foods in ICMR database.
     */
    public int getTotalFoods() {
        return allFoods.size();
    }

    /**
     * Get food by ICMR code (e.g., "A013" for rice).
     */
    public IcmrFood getFoodById(String id) {
        return foodByIdMap.get(id);
    }
}
