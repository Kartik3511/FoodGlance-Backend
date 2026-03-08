package com.kartik.foodglance.model;

public class VisionResult {

    public final String label;
    public final int confidence; // 0–100 percentage

    public VisionResult(String label, int confidence) {
        this.label      = label;
        this.confidence = confidence;
    }
}
