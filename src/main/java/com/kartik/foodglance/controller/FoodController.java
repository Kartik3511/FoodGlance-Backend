package com.kartik.foodglance.controller;

import com.kartik.foodglance.model.FoodResponse;
import com.kartik.foodglance.model.NutritionData;
import com.kartik.foodglance.model.VisionResult;
import com.kartik.foodglance.service.ImageCompressionService;
import com.kartik.foodglance.service.NutritionService;
import com.kartik.foodglance.service.VisionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FoodController {

    private final VisionService visionService;
    private final NutritionService nutritionService;
    private final ImageCompressionService imageCompressionService;

    public FoodController(VisionService visionService,
                          NutritionService nutritionService,
                          ImageCompressionService imageCompressionService) {
        this.visionService           = visionService;
        this.nutritionService        = nutritionService;
        this.imageCompressionService = imageCompressionService;
    }

    @PostMapping(value = "/detect-food", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FoodResponse> detectFood(
            @RequestParam("image") MultipartFile image) {

        try {
            byte[] imageBytes = imageCompressionService.compressImage(image);
            VisionResult vision = visionService.detectFoodLabel(imageBytes);
            NutritionData nutrition = nutritionService.getNutrition(vision.label);
            return ResponseEntity.ok(new FoodResponse(
                    vision.label,
                    nutrition.getCalories(),
                    nutrition.getProtein(),
                    nutrition.getCarbs(),
                    nutrition.getFat(),
                    vision.confidence + "%"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/nutrition")
    public ResponseEntity<FoodResponse> getNutrition(@RequestParam("food") String food) {
        try {
            NutritionData nutrition = nutritionService.getNutrition(food);
            return ResponseEntity.ok(new FoodResponse(
                    food,
                    nutrition.getCalories(),
                    nutrition.getProtein(),
                    nutrition.getCarbs(),
                    nutrition.getFat(),
                    "—"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}

