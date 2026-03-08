package com.kartik.foodglance.service;

import com.kartik.foodglance.model.CompressionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageCompressionService {

    private static final Logger log = LoggerFactory.getLogger(ImageCompressionService.class);

    private static final int MAX_WIDTH = 800;
    private static final float JPEG_QUALITY = 0.7f;

    /**
     * Accepts a MultipartFile, resizes it to max 800px width (preserving aspect ratio),
     * compresses to JPEG at 0.7 quality, and returns the result as a byte array.
     */
    public CompressionResult compressImage(MultipartFile file) throws IOException {
        long originalBytes = file.getSize();

        BufferedImage original = ImageIO.read(file.getInputStream());

        if (original == null) {
            throw new IOException("Could not decode image. Unsupported format or corrupted file.");
        }

        BufferedImage resized  = resizeIfNeeded(original);
        byte[]        compressed = encodeAsJpeg(resized);

        double originalKb    = originalBytes / 1024.0;
        double compressedKb  = compressed.length / 1024.0;
        int reductionPercent = (int) Math.round((1.0 - (double) compressed.length / originalBytes) * 100);

        String originalStr   = originalKb >= 1024
                ? String.format("%.1fMB", originalKb / 1024)
                : String.format("%.0fKB", originalKb);
        String compressedStr = compressedKb >= 1024
                ? String.format("%.1fMB", compressedKb / 1024)
                : String.format("%.0fKB", compressedKb);

        log.info("Image compressed from {} to {} ({}% reduction)", originalStr, compressedStr, reductionPercent);

        return new CompressionResult(compressed, originalBytes, compressed.length);
    }

    private BufferedImage resizeIfNeeded(BufferedImage image) {
        int originalWidth  = image.getWidth();
        int originalHeight = image.getHeight();

        if (originalWidth <= MAX_WIDTH) {
            return image;
        }

        int newWidth  = MAX_WIDTH;
        int newHeight = (int) Math.round((double) originalHeight * MAX_WIDTH / originalWidth);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    private byte[] encodeAsJpeg(BufferedImage image) throws IOException {
        // If image has transparency (ARGB), fill with white background before JPEG encoding
        if (image.getType() == BufferedImage.TYPE_INT_ARGB
                || image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = rgb;
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG image writer available.");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }
}
