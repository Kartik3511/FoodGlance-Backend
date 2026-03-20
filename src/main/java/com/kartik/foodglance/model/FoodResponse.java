package com.kartik.foodglance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodResponse {

    @JsonProperty("food_name")
    private String foodName;

    private String calories;
    private String protein;
    private String carbs;
    private String fat;
    private String confidence;

    // Data source indicator: "ICMR" or "USDA"
    @JsonProperty("data_source")
    private String dataSource;

    // Compression details
    @JsonProperty("original_image_size_kb")
    private long originalImageSizeKB;

    @JsonProperty("compressed_image_size_kb")
    private long compressedImageSizeKB;

    @JsonProperty("compression_ratio")
    private String compressionRatio;

    // Processing times
    @JsonProperty("compression_time_ms")
    private long compressionTimeMs;

    @JsonProperty("vision_time_ms")
    private long visionTimeMs;

    @JsonProperty("nutrition_time_ms")
    private long nutritionTimeMs;

    @JsonProperty("total_time_ms")
    private long totalTimeMs;
}

