package com.mss301.documentservice.service.extraction.impl;

import com.mss301.documentservice.service.extraction.OcrService;
import com.mss301.documentservice.service.extraction.PdfExtractorService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Transactional
@RequiredArgsConstructor
public class PdfExtractorServiceImpl implements PdfExtractorService {

    @Value("2")
    private int threadPoolSize;

    private final OcrService ocrService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public List<String> extractPages(File pdfFile) throws IOException, InterruptedException, ExecutionException {
        List<String> pages = new ArrayList<>();
        List<Future<PageResult>> futures = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            System.out.println("Processing " + totalPages + " pages from: " + pdfFile.getName());

            int batchSize = Math.min(threadPoolSize, 4);
            for (int startPage = 0; startPage < totalPages; startPage += batchSize) {
                int endPage = Math.min(startPage + batchSize, totalPages);

                List<Future<PageResult>> batchFutures = new ArrayList<>();
                for (int i = startPage; i < endPage; i++) {
                    final int pageIndex = i;
                    Future<PageResult> future = executorService.submit(() -> {
                        try {
                            System.out.println("Processing page " + (pageIndex + 1) + "/" + totalPages);

                            BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 200);

                            String text = ocrService.extractText(image).trim();

                            image.flush();

                            if (!text.isEmpty()) {
                                System.out.println("Successfully processed page " + (pageIndex + 1) +
                                        " (" + text.length() + " characters)");
                                return new PageResult(pageIndex, text);
                            } else {
                                System.out.println("Page " + (pageIndex + 1) + " contains no text");
                                return new PageResult(pageIndex, null);
                            }

                        } catch (Exception e) {
                            System.err.println("Failed to process page " + (pageIndex + 1) + ": " + e.getMessage());
                            return new PageResult(pageIndex, null);
                        }
                    });
                    batchFutures.add(future);
                }

                for (Future<PageResult> future : batchFutures) {
                    try {
                        PageResult result = future.get();
                        if (result.text != null) {
                            // Ensure pages list is large enough
                            while (pages.size() <= result.pageIndex) {
                                pages.add(null);
                            }
                            pages.set(result.pageIndex, result.text);
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting result: " + e.getMessage());
                    }
                }

                System.gc();
                Thread.sleep(500);
            }

            pages.removeIf(page -> page == null);
            System.out.println("Successfully extracted " + pages.size() + " pages with text");
        }

        return pages;
    }

    private static class PageResult {
        final int pageIndex;
        final String text;

        PageResult(int pageIndex, String text) {
            this.pageIndex = pageIndex;
            this.text = text;
        }
    }

}
