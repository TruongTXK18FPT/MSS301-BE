package com.mss301.documentservice.service.chunk.impl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.mss301.documentservice.service.chunk.ChunkDeduplicator;
import com.mss301.documentservice.service.chunk.ChunkSplitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkDeduplicatorImpl implements ChunkDeduplicator {

    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\n\\s*\n");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]\\s+");

    private final ChunkSplitter chunkSplitter;

    @Override
    public List<String> createChunksWithDeduplication(String text, int maxChunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        Set<String> seenChunks = new HashSet<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        // If text is shorter than max chunk size, return as single chunk
        if (text.length() <= maxChunkSize) {
            chunks.add(text);
            return chunks;
        }

        // Try deduplication approach first
        chunks = createChunksWithDedupInternal(text, maxChunkSize, overlap, seenChunks);

        // Fallback: if deduplication removed too many chunks, use simple chunking
        if (chunks.isEmpty() || chunks.size() < text.length() / (maxChunkSize * 2)) {
            log.info("Deduplication too aggressive, falling back to simple chunking");
            chunks = chunkSplitter.createChunks(text, maxChunkSize, overlap);
        }

        // Filter out chunks that are too small to be meaningful
        chunks = chunks.stream()
                .filter(chunk -> chunk.trim().length() >= 50) // Minimum 50 characters
                .collect(java.util.stream.Collectors.toList());

        log.info("Created {} valid chunks after filtering", chunks.size());
        return chunks;
    }

    private List<String> createChunksWithDedupInternal(
            String text, int maxChunkSize, int overlap, Set<String> seenChunks) {
        List<String> chunks = new ArrayList<>();

        // Split into paragraphs first
        String[] paragraphs = PARAGRAPH_BREAK.split(text);

        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();

            if (paragraph.isEmpty()) {
                continue;
            }

            // If adding this paragraph would exceed chunk size
            if (currentChunk.length() + paragraph.length() > maxChunkSize) {
                // Save current chunk if it has content and is not duplicate
                if (currentChunk.length() > 0) {
                    String chunkContent = currentChunk.toString().trim();
                    String chunkHash = createChunkHash(chunkContent);

                    if (!seenChunks.contains(chunkHash)) {
                        chunks.add(chunkContent);
                        seenChunks.add(chunkHash);
                    }

                    // Create overlap for next chunk with better boundary detection
                    String overlapText = createSmartOverlap(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                }

                // If paragraph itself is too long, split it further
                if (paragraph.length() > maxChunkSize) {
                    List<String> splitParagraph =
                            splitLongParagraphWithDedup(paragraph, maxChunkSize, overlap, seenChunks);

                    for (int i = 0; i < splitParagraph.size(); i++) {
                        if (i == 0 && currentChunk.length() > 0) {
                            // Add first part to current chunk
                            currentChunk.append(" ").append(splitParagraph.get(i));
                            String chunkContent = currentChunk.toString().trim();
                            String chunkHash = createChunkHash(chunkContent);

                            if (!seenChunks.contains(chunkHash)) {
                                chunks.add(chunkContent);
                                seenChunks.add(chunkHash);
                            }
                            currentChunk = new StringBuilder();
                        } else if (i == splitParagraph.size() - 1) {
                            // Keep last part for next iteration
                            currentChunk.append(splitParagraph.get(i));
                        } else {
                            // Add middle parts as separate chunks
                            String chunkHash = createChunkHash(splitParagraph.get(i));
                            if (!seenChunks.contains(chunkHash)) {
                                chunks.add(splitParagraph.get(i));
                                seenChunks.add(chunkHash);
                            }
                        }
                    }
                } else {
                    currentChunk.append(paragraph);
                }
            } else {
                // Add paragraph to current chunk
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }

        // Add final chunk
        if (currentChunk.length() > 0) {
            String chunkContent = currentChunk.toString().trim();
            String chunkHash = createChunkHash(chunkContent);

            if (!seenChunks.contains(chunkHash)) {
                chunks.add(chunkContent);
                seenChunks.add(chunkHash);
            }
        }

        return chunks;
    }

    /**
     * Create a hash for chunk content to detect duplicates
     */
    public String createChunkHash(String content) {
        // Normalize content and create hash based on meaningful words
        String normalized = content.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s]", "") // Remove punctuation
                .trim();

        // For short content, use the whole content
        if (normalized.length() <= 100) {
            return normalized;
        }

        // For longer content, use a more sophisticated hash
        // Take first 30, middle 30, and last 30 characters + word count
        String start = normalized.substring(0, Math.min(30, normalized.length()));
        String middle = "";
        String end = "";

        if (normalized.length() > 60) {
            int midPoint = normalized.length() / 2;
            middle = normalized.substring(midPoint - 15, midPoint + 15);
            end = normalized.substring(Math.max(0, normalized.length() - 30));
        } else {
            end = normalized.substring(Math.max(0, normalized.length() - 30));
        }

        // Count meaningful words (> 2 characters)
        long wordCount = Arrays.stream(normalized.split("\\s+"))
                .filter(word -> word.length() > 2)
                .count();

        return start + "|" + middle + "|" + end + "|" + wordCount;
    }

    /**
     * Create smart overlap that starts at word boundaries
     */
    private String createSmartOverlap(String previousChunk, int overlapSize) {
        if (previousChunk.length() <= overlapSize) {
            return previousChunk;
        }

        String overlapText = previousChunk.substring(previousChunk.length() - overlapSize);

        // Try to start from a sentence boundary
        Matcher sentenceMatcher = SENTENCE_END.matcher(overlapText);
        if (sentenceMatcher.find()) {
            return overlapText.substring(sentenceMatcher.end()).trim();
        }

        // Try to start from a word boundary
        int firstSpace = overlapText.indexOf(' ');
        if (firstSpace > 0 && firstSpace < overlapSize / 2) {
            return overlapText.substring(firstSpace + 1).trim();
        }

        return overlapText.trim();
    }

    /**
     * Split long paragraph with deduplication
     */
    private List<String> splitLongParagraphWithDedup(
            String paragraph, int maxChunkSize, int overlap, Set<String> seenChunks) {
        List<String> chunks = new ArrayList<>();

        // Split by sentences
        Matcher sentenceMatcher = SENTENCE_END.matcher(paragraph);
        List<String> sentences = new ArrayList<>();

        int lastEnd = 0;
        while (sentenceMatcher.find()) {
            sentences.add(paragraph.substring(lastEnd, sentenceMatcher.end()).trim());
            lastEnd = sentenceMatcher.end();
        }

        // Add remaining text as last sentence
        if (lastEnd < paragraph.length()) {
            sentences.add(paragraph.substring(lastEnd).trim());
        }

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    String chunkContent = currentChunk.toString().trim();
                    String chunkHash = createChunkHash(chunkContent);

                    if (!seenChunks.contains(chunkHash)) {
                        chunks.add(chunkContent);
                        seenChunks.add(chunkHash);
                    }

                    // Create overlap
                    String overlapText = createSmartOverlap(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                }

                // If single sentence is still too long, split by character count
                if (sentence.length() > maxChunkSize) {
                    List<String> splitSentence =
                            splitByCharacterCountWithDedup(sentence, maxChunkSize, overlap, seenChunks);
                    chunks.addAll(splitSentence);
                    currentChunk = new StringBuilder();
                } else {
                    currentChunk.append(sentence);
                }
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            }
        }

        if (currentChunk.length() > 0) {
            String chunkContent = currentChunk.toString().trim();
            String chunkHash = createChunkHash(chunkContent);

            if (!seenChunks.contains(chunkHash)) {
                chunks.add(chunkContent);
                seenChunks.add(chunkHash);
            }
        }

        return chunks;
    }

    /**
     * Last resort: split by character count with word boundaries and deduplication
     */
    private List<String> splitByCharacterCountWithDedup(
            String text, int maxChunkSize, int overlap, Set<String> seenChunks) {
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChunkSize, text.length());

            // Try to break at word boundary
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start + maxChunkSize / 2) {
                    end = lastSpace;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                String chunkHash = createChunkHash(chunk);
                if (!seenChunks.contains(chunkHash)) {
                    chunks.add(chunk);
                    seenChunks.add(chunkHash);
                }
            }

            // Move start position with overlap
            start = Math.max(start + 1, end - overlap);
        }

        return chunks;
    }
}
