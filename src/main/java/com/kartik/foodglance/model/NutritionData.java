package com.kartik.foodglance.model;

public class NutritionData {

    private String calories;
    private String protein;
    private String carbs;
    private String fat;
    private String source; // "ICMR" or "USDA"

    public NutritionData(String calories, String protein, String carbs, String fat) {
        this.calories = calories;
        this.protein  = protein;
        this.carbs    = carbs;
        this.fat      = fat;
        this.source   = "USDA"; // Default to USDA for backwards compatibility
    }

    public NutritionData(String calories, String protein, String carbs, String fat, String source) {
        this.calories = calories;
        this.protein  = protein;
        this.carbs    = carbs;
        this.fat      = fat;
        this.source   = source;
    }

    public String getCalories() { return calories; }
    public String getProtein()  { return protein;  }
    public String getCarbs()    { return carbs;    }
    public String getFat()      { return fat;      }
    public String getSource()   { return source;   }
    
    public void setSource(String source) { this.source = source; }
}
