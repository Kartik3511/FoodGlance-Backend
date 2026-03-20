package com.kartik.foodglance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root model for ICMR-NIN IFCT 2017 JSON data.
 * Contains metadata and list of all foods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IcmrFoodDatabase {

    private Metadata metadata;
    private List<IcmrFood> foods;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String source;
        private String version;
        
        @JsonProperty("extractedDate")
        private String extractedDate;
        
        @JsonProperty("totalFoods")
        private int totalFoods;
        
        @JsonProperty("servingSizeStandard")
        private String servingSizeStandard;
        
        @JsonProperty("energyUnit")
        private String energyUnit;
        
        private String note;
    }
}
