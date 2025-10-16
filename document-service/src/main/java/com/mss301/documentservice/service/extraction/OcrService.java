package com.mss301.documentservice.service.extraction;

import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public interface OcrService {
    String extractText(BufferedImage image) throws TesseractException;
}
