package com.kartik.foodglance.service;

import com.kartik.foodglance.model.VisionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VisionService {

    private static final String VISION_API_URL =
            "https://vision.googleapis.com/v1/images:annotate";

    @Value("${google.vision.api-key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    // Specific food labels we want to return
    private static final Set<String> FOOD_KEYWORDS = Set.of(
        "fruit", "vegetable", "meat", "fish", "seafood", "bread",
        "rice", "pasta", "salad", "soup", "sandwich", "pizza", "burger",
        "cake", "cookie", "chocolate", "cheese", "egg", "chicken",
        "beef", "pork", "noodle", "curry", "stew", "roast",
        "omelette", "pancake", "waffle", "cereal", "yogurt", "sushi",
        "taco", "burrito", "dumpling", "ramen", "steak", "fries",
        "muffin", "brownie", "croissant", "bagel", "donut", "pretzel",
        "tofu", "tempeh", "shrimp", "lobster", "crab", "salmon",
        "apple", "banana", "mango", "strawberry", "grape", "orange",
        "carrot", "broccoli", "tomato", "potato", "onion", "garlic",
        "lemon", "avocado", "spinach", "mushroom", "corn", "pepper",
        "peanut", "nut", "almond", "cashew", "walnut", "pistachio",
        "chickpea", "bean", "lentil", "seed", "popcorn", "granola",
        "dal", "chapati", "paratha", "biryani", "samosa", "paneer",
        "bhakri", "puri", "dosa", "idli", "vada", "upma", "poha"
    );

    // Generic labels that are too vague to name a food, but still CONFIRM food is present.
    // If any of these appear, we should NOT reject the image as non-food.
    private static final Set<String> FOOD_CONTEXT_LABELS = Set.of(
        "food", "dish", "meal", "cuisine", "ingredient", "recipe",
        "produce", "fast food", "comfort food", "finger food",
        "junk food", "staple food", "whole food", "superfood",
        "garnish", "condiment", "snack"
    );

    // Non-food objects — only skip these for the purpose of label selection,
    // but their presence alone does NOT mean the image has no food.
    private static final Set<String> SKIP_LABELS = Set.of(
        "tableware", "plate", "table", "bowl", "cup",
        "glass", "fork", "knife", "spoon", "cutlery", "serveware",
        "dishware", "drinkware",
        "flavor", "taste",
        "texture", "color", "pattern", "wood", "surface", "background",
        "food storage containers", "food storage container",
        "container", "storage", "packaging", "plastic", "ceramic",
        "tabletop", "still life", "snack"
    );

    // Maps generic Vision labels to correct regional food names
    private static final Map<String, String> FOOD_NAME_MAP = Map.ofEntries(
        // Indian breads
        Map.entry("flatbread",       "Chapati"),
        Map.entry("roti",            "Chapati"),
        Map.entry("bhakri",          "Bhakri"),
        Map.entry("millet bread",    "Bhakri"),
        Map.entry("jowar",           "Bhakri"),
        Map.entry("bajra",           "Bhakri"),
        Map.entry("naan",            "Naan bread"),
        Map.entry("paratha",         "Paratha"),
        Map.entry("puri",            "Puri"),
        Map.entry("poori",           "Puri"),
        // Indian mains
        Map.entry("legume dish",     "Dal"),
        Map.entry("lentil soup",     "Dal"),
        Map.entry("lentil",          "Dal"),
        Map.entry("curry",           "Chicken curry"),
        Map.entry("fried pastry",    "Samosa"),
        Map.entry("stuffed pastry",  "Samosa"),
        Map.entry("rice dish",       "Biryani"),
        Map.entry("pilaf",           "Biryani"),
        // Nuts and snacks
        Map.entry("peanut",          "Peanuts"),
        Map.entry("groundnut",       "Peanuts"),
        Map.entry("almond",          "Almonds"),
        Map.entry("cashew",          "Cashews"),
        Map.entry("walnut",          "Walnuts"),
        Map.entry("fried egg",       "Fried egg"),
        Map.entry("french fries",    "French fries"),
        Map.entry("mashed potato",   "Mashed potatoes"),
        Map.entry("grilled chicken", "Grilled chicken"),
        Map.entry("roast chicken",   "Roast chicken")
    );

    public VisionResult detectFoodLabel(byte[] imageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
            "requests", List.of(Map.of(
                "image",    Map.of("content", base64Image),
                "features", List.of(Map.of(
                    "type",       "LABEL_DETECTION",
                    "maxResults", 10
                ))
            ))
        );

        // Explicitly set Content-Type: application/json so Google accepts the request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = VISION_API_URL + "?key=" + apiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.postForObject(url, entity, Map.class);

            if (response == null) return new VisionResult("Unknown Food", 0);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> responses =
                    (List<Map<String, Object>>) response.get("responses");

            if (responses == null || responses.isEmpty()) return new VisionResult("Unknown Food", 0);

            Map<String, Object> firstResponse = responses.get(0);
            if (firstResponse == null) return new VisionResult("Unknown Food", 0);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> labels =
                    (List<Map<String, Object>>) firstResponse.get("labelAnnotations");

            if (labels == null || labels.isEmpty()) return new VisionResult("Unknown Food", 0);

            // Pass 1: find a label that is a specific known food (not in the skip list)
            for (Map<String, Object> label : labels) {
                Object descObj  = label.get("description");
                Object scoreObj = label.get("score");
                if (descObj == null) continue;
                String lower = descObj.toString().toLowerCase();
                if (SKIP_LABELS.contains(lower)) continue;
                for (String keyword : FOOD_KEYWORDS) {
                    if (lower.contains(keyword)) {
                        return new VisionResult(applyFoodNameMap(descObj.toString()), toPercent(scoreObj));
                    }
                }
            }

            // Pass 2: any label that isn't a generic skip label
            for (Map<String, Object> label : labels) {
                Object descObj  = label.get("description");
                Object scoreObj = label.get("score");
                if (descObj == null) continue;
                String lower = descObj.toString().toLowerCase();
                if (!SKIP_LABELS.contains(lower) && !FOOD_CONTEXT_LABELS.contains(lower)) {
                    return new VisionResult(applyFoodNameMap(descObj.toString()), toPercent(scoreObj));
                }
            }

            // Pass 3: if any label confirms food context (e.g. "dish", "meal"), food IS present
            // even if we couldn't identify it specifically — don't reject the image.
            for (Map<String, Object> label : labels) {
                Object descObj = label.get("description");
                if (descObj == null) continue;
                if (FOOD_CONTEXT_LABELS.contains(descObj.toString().toLowerCase())) {
                    // Use the first non-skip label as the best guess food name
                    for (Map<String, Object> l2 : labels) {
                        Object d = l2.get("description");
                        Object s = l2.get("score");
                        if (d == null) continue;
                        if (!SKIP_LABELS.contains(d.toString().toLowerCase())) {
                            return new VisionResult(applyFoodNameMap(d.toString()), toPercent(s));
                        }
                    }
                }
            }

            // No food signals found at all — truly not a food image
            return new VisionResult("NOT_FOOD", 0);

        } catch (HttpClientErrorException e) {
            System.err.println("Vision API error " + e.getStatusCode()
                    + ": " + e.getResponseBodyAsString());
            throw new RuntimeException("Vision API rejected the request: " + e.getStatusCode());

        } catch (RestClientException e) {
            System.err.println("Vision API network error: " + e.getMessage());
            throw new RuntimeException("Could not reach Vision API: " + e.getMessage());
        }
    }

    private int toPercent(Object score) {
        if (score == null) return 0;
        try {
            return (int) Math.round(Double.parseDouble(score.toString()) * 100);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Converts generic Vision labels to correct food names using the mapping table.
    // Uses contains() so variations like "fried pastry dish" still match "fried pastry".
    private String applyFoodNameMap(String label) {
        String lower = label.toLowerCase();
        for (Map.Entry<String, String> entry : FOOD_NAME_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return label;
    }
}
