package com.mss301.documentservice.service.analysis.impl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mss301.documentservice.service.analysis.TableOfContentService;
import com.mss301.documentservice.service.analysis.models.toc.PageMapping;
import com.mss301.documentservice.service.analysis.models.toc.TocEntry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableOfContentServiceImpl implements TableOfContentService {

    private static final int MAX_TOC_LENGTH = 3000;
    private static final int MIN_VALID_PAGE = 1;
    private static final int MAX_VALID_PAGE = 200;
    private static final int MIN_TITLE_LENGTH = 3;
    private static final int LOOKAHEAD_LINES = 4;

    private static final Pattern CHAPTER_PATTERN =
            Pattern.compile("Chương\\s+(\\d+)\\s+([^\\d]+?)\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern LESSON_PATTERN =
            Pattern.compile("Bài\\s+(\\d+)\\s+([^\\d]+?)\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern FLEXIBLE_CHAPTER =
            Pattern.compile("Chương\\s+(\\d+)\\s+([^\\n\\d]+?)(?:\\s+(\\d+))?", Pattern.CASE_INSENSITIVE);

    private static final Pattern FLEXIBLE_LESSON =
            Pattern.compile("Bài\\s+(\\d+)\\s+([^\\n]+?)\\s+(\\d{2,})(?=\\s|$|\\n)", Pattern.CASE_INSENSITIVE);

    private static final Pattern LINE_LESSON =
            Pattern.compile("Bài\\s+(\\d+)\\s+(.+?)\\s+(\\d{1,})$", Pattern.CASE_INSENSITIVE);

    private static final String[] TOC_MARKERS = {
        "Mục lục", "MỤC LỤC", "mục lục", "Table of Contents", "TABLE OF CONTENTS"
    };

    private static final String[] CONTENT_MARKERS = {
        "Phần SỐ VÀ ĐẠI SỐ", "Chương này ôn tập", "HƯỚNG DẪN SỬ DỤNG", "Lời nói đầu"
    };

    @Override
    public List<TocEntry> parseTableOfContents(String fullText) {
        List<TocEntry> entries = new ArrayList<>();

        String tocSection = extractTocSection(fullText);
        if (tocSection == null || tocSection.trim().isEmpty()) {
            log.warn("No table of contents section found");
            return entries;
        }

        log.info("Found TOC section: {}", tocSection.substring(0, Math.min(500, tocSection.length())));

        parseChapters(tocSection, entries);
        parseLessons(tocSection, entries);

        entries.sort(Comparator.comparingInt(TocEntry::getPageNumber));
        assignLessonsToChapters(entries);

        logParsedEntries(entries);
        return entries;
    }

    @Override
    public Map<Integer, PageMapping> createPageMapping(List<TocEntry> tocEntries, int totalPages) {
        if (tocEntries.isEmpty()) {
            return new HashMap<>();
        }

        List<TocEntry> sortedEntries = new ArrayList<>(tocEntries);
        sortedEntries.sort(Comparator.comparingInt(TocEntry::getPageNumber));
        assignLessonsToChapters(sortedEntries);

        List<TocEntry> chapters = filterByType(sortedEntries, "chapter");
        List<TocEntry> lessons = filterByType(sortedEntries, "lesson");

        Map<Integer, PageMapping> pageMap = buildPageMapping(chapters, lessons, totalPages);

        log.info("Created page mapping for {} pages", pageMap.size());
        logSampleMappings(pageMap);

        return pageMap;
    }

    private List<TocEntry> filterByType(List<TocEntry> entries, String type) {
        return entries.stream()
                .filter(e -> type.equals(e.getType()))
                .sorted(Comparator.comparingInt(TocEntry::getPageNumber))
                .collect(Collectors.toList());
    }

    private Map<Integer, PageMapping> buildPageMapping(
            List<TocEntry> chapters, List<TocEntry> lessons, int totalPages) {
        Map<Integer, PageMapping> pageMap = new HashMap<>();

        for (int page = 1; page <= totalPages; page++) {
            TocEntry currentChapter = findCurrentEntry(page, chapters);
            TocEntry currentLesson = findCurrentLesson(page, lessons);

            PageMapping mapping = createPageMapping(currentChapter, currentLesson);
            pageMap.put(page, mapping);
        }

        return pageMap;
    }

    private PageMapping createPageMapping(TocEntry chapter, TocEntry lesson) {
        Integer chapterNum = chapter != null ? chapter.getNumber() : null;
        String chapterTitle = chapter != null ? chapter.getTitle() : null;
        Integer lessonNum = lesson != null ? lesson.getNumber() : null;
        String lessonTitle = lesson != null ? lesson.getTitle() : null;
        String lessonId = (lesson != null && chapter != null)
                ? String.format("chapter_%d_lesson_%d", chapter.getNumber(), lesson.getNumber())
                : null;

        return new PageMapping(chapterNum, chapterTitle, lessonNum, lessonTitle, lessonId);
    }

    private TocEntry findCurrentEntry(int page, List<TocEntry> entries) {
        TocEntry current = null;
        for (TocEntry entry : entries) {
            if (entry.getPageNumber() <= page) {
                current = entry;
            } else {
                break;
            }
        }
        return current;
    }

    private TocEntry findCurrentLesson(int page, List<TocEntry> lessons) {
        for (int i = 0; i < lessons.size(); i++) {
            TocEntry lesson = lessons.get(i);
            TocEntry nextLesson = (i + 1 < lessons.size()) ? lessons.get(i + 1) : null;

            if (lesson.getPageNumber() <= page) {
                if (nextLesson == null || page < nextLesson.getPageNumber()) {
                    return lesson;
                }
            } else {
                break;
            }
        }
        return null;
    }

    private void assignLessonsToChapters(List<TocEntry> sortedEntries) {
        List<TocEntry> chapters = filterByType(sortedEntries, "chapter");

        log.info(
                "Starting lesson-to-chapter assignment for {} entries ({} chapters)",
                sortedEntries.size(),
                chapters.size());

        Integer currentChapter = null;
        for (TocEntry entry : sortedEntries) {
            if ("chapter".equals(entry.getType())) {
                currentChapter = entry.getNumber();
                log.info("Found chapter {}: {} (page {})", entry.getNumber(), entry.getTitle(), entry.getPageNumber());
            } else if ("lesson".equals(entry.getType())) {
                Integer assigned = findChapterForLessonByPage(entry, chapters);
                if (assigned != null) {
                    entry.setParentChapter(assigned);
                    log.info(
                            "Assigned lesson {} '{}' (page {}) to chapter {}",
                            entry.getNumber(),
                            entry.getTitle(),
                            entry.getPageNumber(),
                            assigned);
                } else if (currentChapter != null) {
                    entry.setParentChapter(currentChapter);
                    log.info("Fallback assignment: lesson {} to chapter {}", entry.getNumber(), currentChapter);
                } else {
                    log.warn("Lesson {} has no preceding chapter", entry.getNumber());
                }
            }
        }

        assignOrphanedLessons(sortedEntries);
    }

    private Integer findChapterForLessonByPage(TocEntry lesson, List<TocEntry> chapters) {
        if (chapters.isEmpty()) return null;

        TocEntry bestChapter = null;
        for (TocEntry chapter : chapters) {
            if (chapter.getPageNumber() <= lesson.getPageNumber()) {
                if (bestChapter == null || chapter.getPageNumber() > bestChapter.getPageNumber()) {
                    bestChapter = chapter;
                }
            }
        }
        return bestChapter != null ? bestChapter.getNumber() : null;
    }

    private void assignOrphanedLessons(List<TocEntry> sortedEntries) {
        List<TocEntry> chapters = filterByType(sortedEntries, "chapter");
        if (chapters.isEmpty()) {
            log.warn("No chapters found for orphaned lesson assignment");
            return;
        }

        for (TocEntry lesson : sortedEntries) {
            if ("lesson".equals(lesson.getType()) && lesson.getParentChapter() == null) {
                TocEntry assignedChapter = findChapterForLesson(lesson, chapters);
                if (assignedChapter != null) {
                    lesson.setParentChapter(assignedChapter.getNumber());
                    log.info(
                            "Alternative assignment: lesson {} → chapter {} (page {})",
                            lesson.getNumber(),
                            assignedChapter.getNumber(),
                            assignedChapter.getPageNumber());
                } else {
                    log.error("Could not assign lesson {} to any chapter", lesson.getNumber());
                }
            }
        }
    }

    private TocEntry findChapterForLesson(TocEntry lesson, List<TocEntry> chapters) {
        TocEntry bestChapter = null;
        for (TocEntry chapter : chapters) {
            if (chapter.getPageNumber() <= lesson.getPageNumber()) {
                bestChapter = chapter;
            } else {
                break;
            }
        }

        if (bestChapter == null && !chapters.isEmpty()) {
            bestChapter = chapters.get(0);
            log.warn("Lesson {} comes before all chapters, assigning to first chapter", lesson.getNumber());
        }

        return bestChapter;
    }

    private String extractTocSection(String fullText) {
        int tocStart = findTocStart(fullText);
        if (tocStart == -1) return null;

        int tocEnd = findTocEnd(fullText, tocStart);
        int maxEnd = Math.min(tocEnd, tocStart + MAX_TOC_LENGTH);

        return fullText.substring(tocStart, maxEnd);
    }

    private int findTocStart(String fullText) {
        for (String marker : TOC_MARKERS) {
            int index = fullText.indexOf(marker);
            if (index != -1) return index;
        }

        return findTocByPattern(fullText);
    }

    private int findTocByPattern(String fullText) {
        Pattern chapterPattern = Pattern.compile("Chương\\s+\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = chapterPattern.matcher(fullText);

        int chapterCount = 0;
        int firstChapter = -1;

        while (matcher.find() && chapterCount < 10) {
            if (firstChapter == -1) firstChapter = matcher.start();
            chapterCount++;
            if (chapterCount >= 2) return firstChapter;
        }

        return -1;
    }

    private int findTocEnd(String fullText, int tocStart) {
        int tocEnd = fullText.length();
        for (String marker : CONTENT_MARKERS) {
            int index = fullText.indexOf(marker, tocStart + 100);
            if (index != -1 && index < tocEnd) {
                tocEnd = index;
            }
        }
        return tocEnd;
    }

    private void parseChapters(String tocSection, List<TocEntry> entries) {
        Set<Integer> seenChapters = new HashSet<>();
        Matcher matcher = FLEXIBLE_CHAPTER.matcher(tocSection);

        while (matcher.find()) {
            parseChapterMatch(matcher, tocSection, entries, seenChapters);
        }

        if (entries.stream().noneMatch(e -> "chapter".equals(e.getType()))) {
            parseSimpleChapters(tocSection, entries, seenChapters);
        }
    }

    private void parseChapterMatch(Matcher matcher, String tocSection, List<TocEntry> entries, Set<Integer> seen) {
        try {
            int number = Integer.parseInt(matcher.group(1));
            if (seen.contains(number)) {
                log.warn("Duplicate chapter {} found, skipping", number);
                return;
            }

            String title = cleanTitle(matcher.group(2));
            String pageStr = matcher.group(3);
            int pageNumber = pageStr != null ? Integer.parseInt(pageStr) : findPageNumber(tocSection, matcher.end());

            if (!isValidChapterTitle(title)) {
                log.warn("Skipping invalid chapter title: '{}'", title);
                return;
            }

            seen.add(number);
            entries.add(new TocEntry("chapter", number, title, pageNumber));
            log.debug("Parsed chapter {}: {} (page {})", number, title, pageNumber);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse chapter: {}", matcher.group());
        }
    }

    private int findPageNumber(String tocSection, int position) {
        String remaining = tocSection.substring(position);
        Pattern pagePattern = Pattern.compile("^\\s*(\\d+)");
        Matcher pageMatcher = pagePattern.matcher(remaining);
        return pageMatcher.find() ? Integer.parseInt(pageMatcher.group(1)) : 1;
    }

    private boolean isValidChapterTitle(String title) {
        return title.length() >= MIN_TITLE_LENGTH
                && !title.matches("^\\d+$")
                && !title.toLowerCase().contains("om hur");
    }

    private void parseSimpleChapters(String tocSection, List<TocEntry> entries, Set<Integer> seen) {
        Pattern simpleChapter = Pattern.compile(
                "Chương\\s+(\\d+)\\s+([A-ZÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬĐÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỬỮỰỲÝỶỸỴ\\s]+?)\\s+(\\d+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = simpleChapter.matcher(tocSection);

        while (matcher.find()) {
            try {
                int number = Integer.parseInt(matcher.group(1));
                String title = cleanTitle(matcher.group(2));
                int pageNumber = Integer.parseInt(matcher.group(3));

                if (seen.contains(number) || title.length() < MIN_TITLE_LENGTH) continue;

                seen.add(number);
                entries.add(new TocEntry("chapter", number, title, pageNumber));
                log.debug("Parsed simple chapter {}: {} (page {})", number, title, pageNumber);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse simple chapter: {}", matcher.group());
            }
        }
    }

    private void parseLessons(String tocSection, List<TocEntry> entries) {
        log.info("=== PARSING LESSONS ===");

        Matcher matcher = FLEXIBLE_LESSON.matcher(tocSection);
        while (matcher.find()) {
            parseLessonMatch(matcher, entries);
        }

        parseMultilineLessons(tocSection, entries);
        log.info("=== END LESSON PARSING ===");
    }

    private void parseLessonMatch(Matcher matcher, List<TocEntry> entries) {
        try {
            int number = Integer.parseInt(matcher.group(1));
            String title = cleanTitle(matcher.group(2));
            int pageNumber = Integer.parseInt(matcher.group(3));

            if (!isValidPage(pageNumber)) {
                log.warn("Skipping lesson {} with invalid page: {}", number, pageNumber);
                return;
            }

            entries.add(new TocEntry("lesson", number, title, pageNumber));
            log.info("✓ Added lesson {}: {} (page {})", number, title, pageNumber);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse lesson: {}", matcher.group());
        }
    }

    private void parseMultilineLessons(String tocSection, List<TocEntry> entries) {
        String[] lines = tocSection.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("Bài ")) continue;

            String reconstructed = reconstructLessonLine(lines, i);
            parseReconstructedLesson(reconstructed, entries);
        }
    }

    private String reconstructLessonLine(String[] lines, int startIndex) {
        String result = lines[startIndex].trim();

        if (result.matches(".*\\s+\\d{2,}$")) return result;

        for (int j = startIndex + 1; j < Math.min(startIndex + LOOKAHEAD_LINES, lines.length); j++) {
            String nextLine = lines[j].trim();
            if (nextLine.isEmpty()) continue;
            if (nextLine.startsWith("Bài ") || nextLine.startsWith("Chương")) break;

            result += " " + nextLine;
            if (result.matches(".*\\s+\\d{2,}$")) break;
        }

        return result;
    }

    private void parseReconstructedLesson(String line, List<TocEntry> entries) {
        Matcher matcher = LINE_LESSON.matcher(line);
        if (!matcher.find()) return;

        try {
            int number = Integer.parseInt(matcher.group(1));
            String title = cleanTitle(matcher.group(2));
            int pageNumber = Integer.parseInt(matcher.group(3));

            if (!isValidPage(pageNumber)) return;

            if (isDuplicateLesson(entries, number, pageNumber)) {
                log.info("Lesson {} already exists, skipping", number);
                return;
            }

            entries.add(new TocEntry("lesson", number, title, pageNumber));
            log.info("✓ Added lesson from reconstructed line {}: {} (page {})", number, title, pageNumber);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse reconstructed lesson: {}", line);
        }
    }

    private boolean isValidPage(int page) {
        return page >= MIN_VALID_PAGE && page <= MAX_VALID_PAGE;
    }

    private boolean isDuplicateLesson(List<TocEntry> entries, int number, int pageNumber) {
        return entries.stream()
                .anyMatch(e ->
                        "lesson".equals(e.getType()) && e.getNumber() == number && e.getPageNumber() == pageNumber);
    }

    private String cleanTitle(String title) {
        return title.replaceAll("\\s+", " ")
                .replaceAll("^[\\s.,-]+", "")
                .replaceAll("[\\s.,-]+$", "")
                .trim();
    }

    private void logParsedEntries(List<TocEntry> entries) {
        log.info("Parsed {} TOC entries", entries.size());
        entries.forEach(entry -> {
            String parentInfo =
                    entry.getParentChapter() != null ? " (parent: " + entry.getParentChapter() + ")" : " (no parent)";
            log.info("  {}{}", entry, parentInfo);
        });
    }

    private void logSampleMappings(Map<Integer, PageMapping> pageMap) {
        log.info("=== PAGE MAPPING SAMPLE ===");
        for (int page = 20; page <= 25; page++) {
            PageMapping mapping = pageMap.get(page);
            if (mapping != null) {
                log.info(
                        "  Page {}: Chapter {} - Lesson {} ({})",
                        page,
                        mapping.getChapterNumber(),
                        mapping.getLessonNumber(),
                        mapping.getLessonTitle());
            } else {
                log.info("  Page {}: NO MAPPING", page);
            }
        }
        log.info("=== END SAMPLE ===");
    }
}
