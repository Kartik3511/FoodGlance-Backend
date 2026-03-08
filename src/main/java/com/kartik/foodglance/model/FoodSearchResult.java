package com.kartik.foodglance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodSearchResult {

    @JsonProperty("food_name")
    private String foodName;

    private String calories;
    private String protein;
    private String carbs;
    private String fat;
}
