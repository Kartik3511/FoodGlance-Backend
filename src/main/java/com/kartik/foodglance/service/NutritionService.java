package com.kartik.foodglance.service;

import com.kartik.foodglance.model.FoodSearchResult;
import com.kartik.foodglance.model.NutritionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;

@Service
public class NutritionService {

    // USDA FoodData Central — free, no credit card required
    private static final String USDA_URL =
            "https://api.nal.usda.gov/fdc/v1/foods/search";

    // USDA nutrient IDs
    private static final int NUTRIENT_CALORIES = 1008;
    private static final int NUTRIENT_PROTEIN   = 1003;
    private static final int NUTRIENT_CARBS     = 1005;
    private static final int NUTRIENT_FAT       = 1004;

    @Value("${usda.api-key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Cacheable(value = "food-search", unless = "#result == null || #result.isEmpty()")
    public List<FoodSearchResult> searchFoods(String query) {
        List<FoodSearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = USDA_URL + "?query=" + encodedQuery
                       + "&pageSize=5&api_key=" + apiKey;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) return results;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> foods =
                    (List<Map<String, Object>>) response.get("foods");

            if (foods == null) return results;

            for (Map<String, Object> food : foods) {
                Object nameObj = food.get("description");
                if (nameObj == null) continue;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> nutrients =
                        (List<Map<String, Object>>) food.get("foodNutrients");

                String calories = "0", protein = "0", carbs = "0", fat = "0";

                if (nutrients != null) {
                    for (Map<String, Object> nutrient : nutrients) {
                        Object idObj  = nutrient.get("nutrientId");
                        Object valObj = nutrient.get("value");
                        if (idObj == null || valObj == null) continue;

                        int id = Integer.parseInt(idObj.toString());
                        String val = formatNumber(valObj);

                        if      (id == NUTRIENT_CALORIES) calories = val;
                        else if (id == NUTRIENT_PROTEIN)  protein  = val;
                        else if (id == NUTRIENT_CARBS)    carbs    = val;
                        else if (id == NUTRIENT_FAT)      fat      = val;
                    }
                }

                results.add(new FoodSearchResult(nameObj.toString(), calories, protein, carbs, fat));
            }

        } catch (Exception e) {
            System.err.println("USDA search error for '" + query + "': " + e.getMessage());
        }
        return results;
    }

    @Cacheable(value = "nutrition", unless = "#result == null")
    public NutritionData getNutrition(String foodName) {
        String cleanedName = cleanFoodName(foodName);
        try {
            String encodedQuery = URLEncoder.encode(cleanedName, StandardCharsets.UTF_8);
            String url = USDA_URL + "?query=" + encodedQuery
                       + "&pageSize=1&api_key=" + apiKey;

            @SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.getForObject(url, Map.class);

            if (response == null) return fallback();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> foods =
                    (List<Map<String, Object>>) response.get("foods");

            if (foods == null || foods.isEmpty()) return fallback();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nutrients =
                    (List<Map<String, Object>>) foods.get(0).get("foodNutrients");

            if (nutrients == null) return fallback();

            String calories = "0", protein = "0", carbs = "0", fat = "0";

            for (Map<String, Object> nutrient : nutrients) {
                Object idObj  = nutrient.get("nutrientId");
                Object valObj = nutrient.get("value");
                if (idObj == null || valObj == null) continue;

                int id = Integer.parseInt(idObj.toString());
                String val = formatNumber(valObj);

                if      (id == NUTRIENT_CALORIES) calories = val;
                else if (id == NUTRIENT_PROTEIN)  protein  = val;
                else if (id == NUTRIENT_CARBS)    carbs    = val;
                else if (id == NUTRIENT_FAT)      fat      = val;
            }

            return new NutritionData(calories, protein, carbs, fat);

        } catch (Exception e) {
            System.err.println("USDA API error for '" + cleanedName + "': " + e.getMessage());
            e.printStackTrace();
            return fallback();
        }
    }

    // Strips vague suffixes so "Egg dish" → "Egg", "Chicken meal" → "Chicken"
    private String cleanFoodName(String foodName) {
        String[] genericSuffixes = {
            " dish", " food", " meal", " item", " product",
            " cuisine", " recipe", " ingredient", " snack"
        };
        String cleaned = foodName.trim();
        String lower   = cleaned.toLowerCase();
        for (String suffix : genericSuffixes) {
            if (lower.endsWith(suffix)) {
                cleaned = cleaned.substring(0, cleaned.length() - suffix.length()).trim();
                break;
            }
        }
        return cleaned.isEmpty() ? foodName : cleaned;
    }

    private String formatNumber(Object value) {
        try {
            double d = Double.parseDouble(value.toString());
            return String.valueOf((int) Math.round(d));
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    private NutritionData fallback() {
        return new NutritionData("0", "0", "0", "0");
    }
}
