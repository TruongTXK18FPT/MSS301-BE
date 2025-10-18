package com.mss301.documentservice.service.chunk.impl;

import com.mss301.documentservice.service.analysis.models.toc.PageMapping;
import com.mss301.documentservice.service.chunk.PageEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class PageEstimatorImpl implements PageEstimator {
    private Map<Integer, PageMapping> tocPageMapping;
    private int totalPages;

    @Override
    public void initializeWithTocMapping(Map<Integer, PageMapping> tocPageMapping, int totalPages) {
        this.tocPageMapping = tocPageMapping;
        this.totalPages = totalPages;

        log.debug("PageEstimator initialized with TOC mapping: {} entries, total pages: {}",
                tocPageMapping != null ? tocPageMapping.size() : 0, totalPages);
    }

    @Override
    public int estimatePageFromPosition(String fullText, int position) {
        if (fullText == null || fullText.isEmpty() || position <= 0) {
            return 1;
        }

        // If we have TOC page mapping, use it for more accurate estimation
        if (tocPageMapping != null && !tocPageMapping.isEmpty()) {
            return estimatePageFromPositionWithTOC(position);
        }

        // Fallback to improved character-based estimation
        return estimatePageFromPositionCharacterBased(fullText, position);
    }

    private int estimatePageFromPositionWithTOC(int position) {
        // Find the closest TOC entry by position
        int bestPage = 1;
        int bestDistance = Integer.MAX_VALUE;

        for (Map.Entry<Integer, PageMapping> entry : tocPageMapping.entrySet()) {
            int page = entry.getKey();
            // Estimate position for this page (rough approximation)
            int estimatedPosition = (page - 1) * 2000; // Assume 2000 chars per page

            int distance = Math.abs(position - estimatedPosition);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPage = page;
            }
        }

        // Fine-tune based on position offset
        int estimatedPositionForBestPage = (bestPage - 1) * 2000;
        if (position > estimatedPositionForBestPage + 1000) {
            // Position is significantly after the estimated position, might be next page
            bestPage = Math.min(bestPage + 1, totalPages);
        } else if (position < estimatedPositionForBestPage - 1000 && bestPage > 1) {
            // Position is significantly before the estimated position, might be previous page
            bestPage = Math.max(bestPage - 1, 1);
        }

        log.debug("TOC-based page estimation for position {}: estimated page {} (distance: {})",
                position, bestPage, bestDistance);

        return bestPage;
    }

    /**
     * Fallback character-based page estimation
     */
    private int estimatePageFromPositionCharacterBased(String fullText, int position) {
        // More sophisticated estimation based on document structure
        // Look for explicit page markers first
        String textUpToPosition = fullText.substring(0, Math.min(position, fullText.length()));

        // Count explicit page markers (like page breaks, page numbers in headers/footers)
        Pattern pageMarker = Pattern.compile("(?m)^\\s*\\d+\\s*$|\\f|\\n\\s*\\n\\s*\\d+\\s*\\n");
        Matcher matcher = pageMarker.matcher(textUpToPosition);

        int explicitPages = 0;
        while (matcher.find()) {
            explicitPages++;
        }

        if (explicitPages > 0) {
            // Use explicit page markers with some adjustment
            return Math.max(1, explicitPages + 1);
        }

        // Account for varying content density
        double avgCharsPerPage = calculateAverageCharsPerPage(textUpToPosition);

        int estimatedPage = Math.max(1, (int) Math.ceil(position / avgCharsPerPage));

        // Add debug logging to understand the estimation
        log.debug("Character-based page estimation for position {}: avgCharsPerPage={}, estimated={}",
                position, avgCharsPerPage, estimatedPage);

        return estimatedPage;
    }

    /**
     * Calculate average characters per page based on text characteristics
     */
    private double calculateAverageCharsPerPage(String textUpToPosition) {
        // Estimate based on text characteristics
        long paragraphs = textUpToPosition.split("\\n\\s*\\n").length;
        long words = textUpToPosition.split("\\s+").length;

        if (words < 1000) {
            // Likely TOC or intro pages - more sparse
            return 1500;
        } else if (paragraphs > words * 0.1) {
            // Lots of short paragraphs - possibly math/exercises
            return 1800;
        } else {
            // Regular text content
            return 2200;
        }
    }

    /**
     * Find the position of a chunk in the original text
     */
    @Override
    public int findChunkPosition(String fullText, String chunkText, int startFrom) {
        // Try to find exact match first
        int position = fullText.indexOf(chunkText, startFrom);
        if (position != -1) {
            return position;
        }

        // If not found, try to find the beginning of the chunk
        String[] words = chunkText.split("\\s+");
        if (words.length > 0) {
            String firstWords = String.join(" ", java.util.Arrays.copyOf(words, Math.min(5, words.length)));
            position = fullText.indexOf(firstWords, startFrom);
            if (position != -1) {
                return position;
            }
        }

        // Fallback to estimated position
        return startFrom;
    }
}
