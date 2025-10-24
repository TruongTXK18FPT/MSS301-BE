package com.mss301.documentservice.service.extraction;

import java.awt.image.BufferedImage;

import net.sourceforge.tess4j.TesseractException;

public interface OcrService {
    String extractText(BufferedImage image) throws TesseractException;
}
