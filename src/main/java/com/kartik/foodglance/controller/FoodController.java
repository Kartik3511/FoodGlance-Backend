package com.kartik.foodglance.controller;

import com.kartik.foodglance.model.CompressionResult;
import com.kartik.foodglance.model.ErrorResponse;
import com.kartik.foodglance.model.FoodResponse;
import com.kartik.foodglance.model.FoodSearchResult;
import com.kartik.foodglance.model.NutritionData;
import com.kartik.foodglance.model.VisionResult;
import com.kartik.foodglance.service.ImageCompressionService;
import com.kartik.foodglance.service.HybridNutritionService;
import com.kartik.foodglance.service.VisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class FoodController {

    private static final Logger log = LoggerFactory.getLogger(FoodController.class);
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    private final VisionService visionService;
    private final HybridNutritionService hybridNutritionService;
    private final ImageCompressionService imageCompressionService;

    public FoodController(VisionService visionService,
                          HybridNutritionService hybridNutritionService,
                          ImageCompressionService imageCompressionService) {
        this.visionService           = visionService;
        this.hybridNutritionService  = hybridNutritionService;
        this.imageCompressionService = imageCompressionService;
    }

    @PostMapping(value = "/detect-food", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> detectFood(
            @RequestParam("image") MultipartFile image,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        log.info("Food detection request from IP: {} | size: {}KB", clientIp, image.getSize() / 1024);

        if (image.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new ErrorResponse(
                            413,
                            "Payload Too Large",
                            "Image exceeds the 5MB limit. Please upload a smaller image."
                    ));
        }

        long totalStart = System.currentTimeMillis();

        try {
            // Step 1: Compress image
            long t1 = System.currentTimeMillis();
            CompressionResult compression = imageCompressionService.compressImage(image);
            long compressionMs = System.currentTimeMillis() - t1;

            // Step 2: Vision API
            long t2 = System.currentTimeMillis();
            VisionResult vision = visionService.detectFoodLabel(compression.bytes);
            long visionMs = System.currentTimeMillis() - t2;

            // Reject non-food images before hitting the nutrition API
            if ("NOT_FOOD".equals(vision.label)) {
                log.info("Non-food image detected — returning 422");
                return ResponseEntity.unprocessableEntity()
                        .body(Map.of("error", "No food detected. Please scan a food item."));
            }

            // Step 3: Hybrid Nutrition Lookup (ICMR → USDA)
            long t3 = System.currentTimeMillis();
            NutritionData nutrition = hybridNutritionService.getNutrition(vision.label);
            long nutritionMs = System.currentTimeMillis() - t3;

            long totalMs = System.currentTimeMillis() - totalStart;

            log.info("Pipeline timing — compression: {}ms | vision: {}ms | nutrition: {}ms | total: {}ms",
                    compressionMs, visionMs, nutritionMs, totalMs);

            FoodResponse response = new FoodResponse();
            response.setFoodName(vision.label);
            response.setCalories(nutrition.getCalories());
            response.setProtein(nutrition.getProtein());
            response.setCarbs(nutrition.getCarbs());
            response.setFat(nutrition.getFat());
            response.setConfidence(vision.confidence + "%");
            response.setDataSource(nutrition.getSource());  // NEW: Add data source
            response.setOriginalImageSizeKB(compression.originalSizeKB);
            response.setCompressedImageSizeKB(compression.compressedSizeKB);
            response.setCompressionRatio(compression.compressionRatio + "%");
            response.setCompressionTimeMs(compressionMs);
            response.setVisionTimeMs(visionMs);
            response.setNutritionTimeMs(nutritionMs);
            response.setTotalTimeMs(totalMs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/foods/search")
    public ResponseEntity<?> searchFoods(@RequestParam("q") String query,
                                         HttpServletRequest request) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(400, "Bad Request", "Query parameter 'q' must not be empty."));
        }
        log.info("Food search request from IP: {} | query: \"{}\"", getClientIp(request), query);
        return ResponseEntity.ok(hybridNutritionService.searchFoods(query));
    }

    @GetMapping("/nutrition")
    public ResponseEntity<?> getNutrition(@RequestParam("food") String food,
                                          HttpServletRequest request) {
        log.info("Nutrition request from IP: {} | food: \"{}\"", getClientIp(request), food);
        try {
            NutritionData nutrition = hybridNutritionService.getNutrition(food);
            FoodResponse response = new FoodResponse();
            response.setFoodName(food);
            response.setCalories(nutrition.getCalories());
            response.setProtein(nutrition.getProtein());
            response.setCarbs(nutrition.getCarbs());
            response.setFat(nutrition.getFat());
            response.setConfidence("—");
            response.setDataSource(nutrition.getSource());  // NEW: Add data source
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/version")
    public String version() {
        return "FoodGlance Backend v1.0";
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

