package com.mss301.documentservice.service.extraction;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

public interface PdfExtractorService {
    List<String> extractPages(File pdfFile) throws IOException, InterruptedException, ExecutionException;
}
