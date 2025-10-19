package com.mss301.documentservice.service.analysis;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.mss301.documentservice.service.analysis.models.structure.DocumentStructure;
import com.mss301.documentservice.service.analysis.models.structure.StructureContext;
import com.mss301.documentservice.service.analysis.models.toc.PageMapping;

public interface DocumentStructureService {
    DocumentStructure analyzeDocumentStructure(String fullText);

    DocumentStructure analyzeDocumentStructure(String fullText, int totalPages);

    Map<Integer, PageMapping> getTocPageMapping();

    int getTotalPages();

    StructureContext findStructureContextWithContentAnalysis(
            DocumentStructure structure, int textPosition, int estimatedPage, String chunkContent);
}
