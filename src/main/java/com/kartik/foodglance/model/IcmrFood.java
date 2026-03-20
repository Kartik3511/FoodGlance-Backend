package com.kartik.foodglance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Model for ICMR-NIN IFCT 2017 food data loaded from JSON.
 * Represents a single food item with its nutritional information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IcmrFood {

    private String id;              // Food code (e.g., "A013", "B002")
    private String name;            // Official English name
    
    @JsonProperty("nameLocal")
    private String nameLocal;       // Local/Hindi name (optional)
    
    private String category;        // Food category (e.g., "Cereals and Millets")
    private List<String> names;     // Search variations for fuzzy matching
    
    @JsonProperty("servingSize")
    private int servingSize;        // Standard serving size (100g for IFCT)
    
    @JsonProperty("servingUnit")
    private String servingUnit;     // Unit (g, ml, etc.)
    
    private Map<String, Object> nutrients;  // Nutrient data (energy, protein, carbs, fat, etc.)

    /**
     * Get nutrient value as Double, with fallback to 0.0 if not present.
     */
    public Double getNutrient(String key) {
        if (nutrients == null || !nutrients.containsKey(key)) {
            return 0.0;
        }
        Object value = nutrients.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get energy in kcal.
     */
    public int getEnergy() {
        return getNutrient("energy").intValue();
    }

    /**
     * Get protein in grams.
     */
    public double getProtein() {
        return getNutrient("protein");
    }

    /**
     * Get carbohydrate in grams.
     */
    public double getCarbohydrate() {
        return getNutrient("carbohydrate");
    }

    /**
     * Get fat in grams.
     */
    public double getFat() {
        return getNutrient("fat");
    }

    /**
     * Get fiber in grams.
     */
    public double getFiber() {
        return getNutrient("fiber");
    }
}
