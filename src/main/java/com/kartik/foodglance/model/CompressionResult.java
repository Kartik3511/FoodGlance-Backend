package com.kartik.foodglance.model;

public class CompressionResult {
    public final byte[] bytes;
    public final long   originalSizeKB;
    public final long   compressedSizeKB;
    public final int    compressionRatio; // % reduction

    public CompressionResult(byte[] bytes, long originalBytes, long compressedBytes) {
        this.bytes            = bytes;
        this.originalSizeKB   = Math.round(originalBytes  / 1024.0);
        this.compressedSizeKB = Math.round(compressedBytes / 1024.0);
        this.compressionRatio = originalBytes > 0
                ? (int) Math.round((1.0 - (double) compressedBytes / originalBytes) * 100)
                : 0;
    }
}
