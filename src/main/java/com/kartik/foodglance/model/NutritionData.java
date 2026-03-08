package com.kartik.foodglance.model;

public class NutritionData {

    private String calories;
    private String protein;
    private String carbs;
    private String fat;

    public NutritionData(String calories, String protein, String carbs, String fat) {
        this.calories = calories;
        this.protein  = protein;
        this.carbs    = carbs;
        this.fat      = fat;
    }

    public String getCalories() { return calories; }
    public String getProtein()  { return protein;  }
    public String getCarbs()    { return carbs;    }
    public String getFat()      { return fat;      }
}
