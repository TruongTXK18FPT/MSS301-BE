package com.mss301.documentservice.service.chunk.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mss301.documentservice.service.chunk.ChunkSplitter;

public class ChunkSplitterImpl implements ChunkSplitter {

    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\n\\s*\n");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]\\s+");
    private static final Pattern SECTION_HEADER =
            Pattern.compile("^(\\d+\\.\\d*|[IVXLC]+\\.|Chương|Bài|Phần)\\s+", Pattern.MULTILINE);

    @Override
    public List<String> createChunks(String text, int maxChunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        // Clean up text
        text = cleanText(text);

        // If text is shorter than max chunk size, return as single chunk
        if (text.length() <= maxChunkSize) {
            chunks.add(text);
            return chunks;
        }

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
                // Save current chunk if it has content
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());

                    // Create overlap for next chunk
                    String overlapText = createOverlap(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                }

                // If paragraph itself is too long, split it further
                if (paragraph.length() > maxChunkSize) {
                    List<String> splitParagraph = splitLongParagraph(paragraph, maxChunkSize, overlap);

                    for (int i = 0; i < splitParagraph.size(); i++) {
                        if (i == 0 && currentChunk.length() > 0) {
                            // Add first part to current chunk
                            currentChunk.append(" ").append(splitParagraph.get(i));
                            chunks.add(currentChunk.toString().trim());
                            currentChunk = new StringBuilder();
                        } else if (i == splitParagraph.size() - 1) {
                            // Keep last part for next iteration
                            currentChunk.append(splitParagraph.get(i));
                        } else {
                            // Add middle parts as separate chunks
                            chunks.add(splitParagraph.get(i));
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
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Split a long paragraph at sentence boundaries
     */
    private List<String> splitLongParagraph(String paragraph, int maxChunkSize, int overlap) {
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
                    chunks.add(currentChunk.toString().trim());

                    // Create overlap
                    String overlapText = createOverlap(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                }

                // If single sentence is still too long, split by character count
                if (sentence.length() > maxChunkSize) {
                    List<String> splitSentence = splitByCharacterCount(sentence, maxChunkSize, overlap);
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
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Last resort: split by character count with word boundaries
     */
    private List<String> splitByCharacterCount(String text, int maxChunkSize, int overlap) {
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
                chunks.add(chunk);
            }

            // Move start position with overlap
            start = Math.max(start + 1, end - overlap);
        }

        return chunks;
    }

    /**
     * Create overlap text from the end of previous chunk
     */
    private String createOverlap(String previousChunk, int overlapSize) {
        if (previousChunk.length() <= overlapSize) {
            return previousChunk;
        }

        String overlapText = previousChunk.substring(previousChunk.length() - overlapSize);

        // Try to start from a sentence boundary
        Matcher sentenceMatcher = SENTENCE_END.matcher(overlapText);
        if (sentenceMatcher.find()) {
            return overlapText.substring(sentenceMatcher.start()).trim();
        }

        // Try to start from a word boundary
        int firstSpace = overlapText.indexOf(' ');
        if (firstSpace > 0) {
            return overlapText.substring(firstSpace + 1).trim();
        }

        return overlapText.trim();
    }

    /**
     * Clean and normalize text
     */
    @Override
    public String cleanText(String text) {
        return text
                // Remove excessive whitespace
                .replaceAll("\\s+", " ")
                // Remove page numbers and headers/footers (simple heuristic)
                .replaceAll("(?m)^\\s*\\d+\\s*$", "")
                // Normalize line breaks
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                // Remove multiple consecutive newlines but preserve paragraph breaks
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * Detect if text contains chapter/section markers
     */
    @Override
    public List<String> detectChapters(String text) {
        List<String> chapters = new ArrayList<>();
        Matcher matcher = SECTION_HEADER.matcher(text);

        List<Integer> chapterStarts = new ArrayList<>();
        while (matcher.find()) {
            chapterStarts.add(matcher.start());
        }

        // Split text by detected chapter boundaries
        for (int i = 0; i < chapterStarts.size(); i++) {
            int start = chapterStarts.get(i);
            int end = (i + 1 < chapterStarts.size()) ? chapterStarts.get(i + 1) : text.length();

            String chapter = text.substring(start, end).trim();
            if (!chapter.isEmpty()) {
                chapters.add(chapter);
            }
        }

        return chapters;
    }
}
