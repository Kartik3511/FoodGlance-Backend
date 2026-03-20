package com.kartik.foodglance.util;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for fuzzy matching of Indian food names.
 * Handles common variations, transliterations, and spelling differences.
 */
@Component
public class FoodNameMatcher {

    private static final LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
    
    // Maximum allowed edit distance for fuzzy match (higher = more lenient)
    private static final int MAX_EDIT_DISTANCE = 3;
    
    // Minimum similarity threshold (0.0 to 1.0, higher = stricter)
    private static final double MIN_SIMILARITY = 0.75;

    /**
     * Normalize Indian food name for comparison.
     */
    public String normalizeIndianFoodName(String foodName) {
        if (foodName == null || foodName.isBlank()) {
            return "";
        }

        String normalized = foodName.toLowerCase().trim();
        normalized = normalized.replaceAll("\\([^)]*\\)", "").trim();
        normalized = normalized.split(",")[0].trim();

        // Normalize common variations
        normalized = normalized
            .replaceAll("daal", "dal")
            .replaceAll("dhal", "dal")
            .replaceAll("chappati", "chapati")
            .replaceAll("chapathi", "chapati")
            .replaceAll("idly", "idli")
            .replaceAll("dosai", "dosa")
            .replaceAll("panir", "paneer")
            .replaceAll("yogurt", "curd")
            .replaceAll("dahi", "curd")
            .replaceAll("chawal", "rice")
            .replaceAll("nachni", "ragi")
            .replaceAll("bhindi", "okra")
            .replaceAll("baingan", "eggplant")
            .replaceAll("aloo", "potato");

        normalized = Pattern.compile("\\s+").matcher(normalized).replaceAll(" ").trim();
        return normalized;
    }

    public int calculateDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        return levenshtein.apply(s1, s2);
    }

    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        int distance = calculateDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) distance / maxLen);
    }

    public boolean isFuzzyMatch(String name1, String name2) {
        String norm1 = normalizeIndianFoodName(name1);
        String norm2 = normalizeIndianFoodName(name2);

        if (norm1.equals(norm2)) return true;
        if (norm1.contains(norm2) || norm2.contains(norm1)) return true;

        int distance = calculateDistance(norm1, norm2);
        if (distance <= MAX_EDIT_DISTANCE) return true;

        double similarity = calculateSimilarity(norm1, norm2);
        return similarity >= MIN_SIMILARITY;
    }

    public double getMatchScore(String query, String candidateName) {
        String normQuery = normalizeIndianFoodName(query);
        String normCandidate = normalizeIndianFoodName(candidateName);

        if (normQuery.equals(normCandidate)) return 1.0;
        if (normCandidate.contains(normQuery)) return 0.95;
        if (normQuery.contains(normCandidate)) return 0.90;

        return calculateSimilarity(normQuery, normCandidate);
    }
}
