package com.mss301.documentservice.service.extraction;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public interface PdfExtractorService {
    List<String> extractPages(File pdfFile);
}
