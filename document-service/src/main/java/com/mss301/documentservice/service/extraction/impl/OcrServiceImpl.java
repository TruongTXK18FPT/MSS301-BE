package com.mss301.documentservice.service.extraction.impl;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.documentservice.service.extraction.OcrService;

import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
@Transactional
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final Tesseract tesseract;

    public OcrServiceImpl() {
        tesseract = new Tesseract();
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("vie+eng");

        tesseract.setPageSegMode(6);
        tesseract.setOcrEngineMode(1);
        tesseract.setVariable("tessedit_pageseg_mode", "6");
        tesseract.setVariable("preserve_interword_spaces", "1");
        tesseract.setVariable("user_defined_dpi", "300");
        tesseract.setVariable("tessedit_create_hocr", "0");
        tesseract.setVariable("tessedit_create_tsv", "0");
    }

    @Override
    public String extractText(BufferedImage image) throws TesseractException {
        synchronized (tesseract) {
            try {
                BufferedImage enhancedImage = enhanceImageForOCR(image);

                return performOCRWithRetry(enhancedImage, 3);

            } catch (Exception e) {
                System.err.println("OCR failed: " + e.getMessage());
                throw new TesseractException("OCR processing failed", e);
            }
        }
    }

    private String performOCRWithRetry(BufferedImage image, int maxRetries) throws TesseractException {
        TesseractException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("OCR attempt " + attempt + "/" + maxRetries);

                // Create a fresh copy of the image for each attempt
                BufferedImage imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
                imageCopy.getGraphics().drawImage(image, 0, 0, null);

                String result = tesseract.doOCR(imageCopy);

                // Clear image immediately
                imageCopy.flush();

                return result;

            } catch (TesseractException e) {
                lastException = e;
                System.err.println("OCR attempt " + attempt + " failed: " + e.getMessage());

                if (attempt < maxRetries) {
                    // Wait before retry
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw lastException != null
                ? lastException
                : new TesseractException("OCR failed after " + maxRetries + " attempts");
    }

    private BufferedImage enhanceImageForOCR(BufferedImage original) {
        BufferedImage grayscale =
                new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        BufferedImage enhanced =
                new BufferedImage(grayscale.getWidth(), grayscale.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 0; x < grayscale.getWidth(); x++) {
            for (int y = 0; y < grayscale.getHeight(); y++) {
                int pixel = grayscale.getRGB(x, y);
                int gray = (pixel >> 16) & 0xff;

                gray = gray > 128 ? 255 : 0;

                int newPixel = (gray << 16) | (gray << 8) | gray;
                enhanced.setRGB(x, y, newPixel);
            }
        }

        return enhanced;
    }
}
