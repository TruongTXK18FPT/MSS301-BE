package com.mss301.documentservice.service.analysis.impl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.mss301.documentservice.service.analysis.DocumentStructureService;
import com.mss301.documentservice.service.analysis.TableOfContentService;
import com.mss301.documentservice.service.analysis.models.structure.ChapterInfo;
import com.mss301.documentservice.service.analysis.models.structure.DocumentStructure;
import com.mss301.documentservice.service.analysis.models.structure.LessonInfo;
import com.mss301.documentservice.service.analysis.models.structure.StructureContext;
import com.mss301.documentservice.service.analysis.models.toc.PageMapping;
import com.mss301.documentservice.service.analysis.models.toc.TocEntry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentStructureServiceImpl implements DocumentStructureService {
    private static final int AVG_CHARS_PER_PAGE = 2000;
    private static final int PAGE_OFFSET_RANGE = 3;
    private static final int MIN_SCORE_THRESHOLD = 15;

    private static final Pattern CHAPTER_PATTERN =
            Pattern.compile("^\\s*Chương\\s+(\\d+)\\s+(.*)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Pattern LESSON_PATTERN =
            Pattern.compile("^\\s*Bài\\s+(\\d+)\\s+(.*)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private final TableOfContentService tableOfContentService;

    private Map<Integer, PageMapping> tocPageMapping;
    private int totalPages;

    @Override
    public DocumentStructure analyzeDocumentStructure(String fullText) {
        return analyzeDocumentStructure(fullText, -1);
    }

    @Override
    public DocumentStructure analyzeDocumentStructure(String fullText, int totalPages) {
        this.totalPages = totalPages > 0 ? totalPages : estimateTotalPages(fullText);

        List<TocEntry> tocEntries = tableOfContentService.parseTableOfContents(fullText);

        if (!tocEntries.isEmpty()) {
            log.info("Using TOC for structure analysis with {} pages", this.totalPages);
            this.tocPageMapping = tableOfContentService.createPageMapping(tocEntries, this.totalPages);
            return buildStructureFromToc(tocEntries, fullText);
        }

        log.info("No TOC found, using content analysis");
        this.tocPageMapping = null;
        return buildStructureFromContent(fullText);
    }

    private DocumentStructure buildStructureFromToc(List<TocEntry> tocEntries, String fullText) {
        Map<Integer, ChapterInfo> chapterMap = new HashMap<>();
        log.info("Building structure from {} TOC entries", tocEntries.size());

        for (TocEntry entry : tocEntries) {
            if ("chapter".equals(entry.getType())) {
                createChapterFromToc(entry, chapterMap);
            } else if ("lesson".equals(entry.getType())) {
                createLessonFromToc(entry, chapterMap);
            }
        }

        List<ChapterInfo> chapters = finalizeStructure(chapterMap, fullText.length());
        logStructureSummary(chapters);

        DocumentStructure structure = new DocumentStructure();
        structure.setChapters(chapters);
        return structure;
    }

    private void createChapterFromToc(TocEntry entry, Map<Integer, ChapterInfo> chapterMap) {
        ChapterInfo chapter = new ChapterInfo();
        chapter.setNumber(entry.getNumber());
        chapter.setTitle(entry.getTitle());
        chapter.setStartPosition(estimatePositionFromPage(entry.getPageNumber()));
        chapter.setEndPosition(-1);
        chapterMap.put(entry.getNumber(), chapter);
        log.info("Created chapter {}: {}", entry.getNumber(), entry.getTitle());
    }

    private void createLessonFromToc(TocEntry entry, Map<Integer, ChapterInfo> chapterMap) {
        LessonInfo lesson = new LessonInfo();
        lesson.setNumber(entry.getNumber());
        lesson.setTitle(entry.getTitle());
        lesson.setStartPosition(estimatePositionFromPage(entry.getPageNumber()));
        lesson.setEndPosition(-1);

        Integer parentChapter = entry.getParentChapter();
        if (parentChapter != null) {
            assignLessonToChapter(lesson, parentChapter, chapterMap);
        } else {
            handleOrphanLesson(lesson, chapterMap);
        }
    }

    private void assignLessonToChapter(LessonInfo lesson, Integer chapterNumber, Map<Integer, ChapterInfo> chapterMap) {
        lesson.setChapterNumber(chapterNumber);
        lesson.setLessonId(generateLessonId(chapterNumber, lesson.getNumber()));

        ChapterInfo chapter = chapterMap.computeIfAbsent(chapterNumber, num -> {
            log.warn("Chapter {} not found, creating placeholder", num);
            ChapterInfo placeholder = new ChapterInfo();
            placeholder.setNumber(num);
            placeholder.setTitle("Chapter " + num);
            placeholder.setStartPosition(0);
            placeholder.setEndPosition(-1);
            return placeholder;
        });

        chapter.getLessons().add(lesson);
        log.info("Added lesson {} to chapter {}", lesson.getNumber(), chapterNumber);
    }

    private void handleOrphanLesson(LessonInfo lesson, Map<Integer, ChapterInfo> chapterMap) {
        log.warn("Lesson {} has no parent chapter", lesson.getNumber());

        Integer targetChapter =
                chapterMap.isEmpty() ? 0 : chapterMap.keySet().iterator().next();
        ChapterInfo chapter = chapterMap.computeIfAbsent(targetChapter, num -> {
            ChapterInfo defaultChapter = new ChapterInfo();
            defaultChapter.setNumber(num);
            defaultChapter.setTitle(num == 0 ? "Miscellaneous" : "Chapter " + num);
            defaultChapter.setStartPosition(0);
            defaultChapter.setEndPosition(-1);
            return defaultChapter;
        });

        lesson.setChapterNumber(targetChapter);
        lesson.setLessonId(generateLessonId(targetChapter, lesson.getNumber()));
        chapter.getLessons().add(lesson);
    }

    private List<ChapterInfo> finalizeStructure(Map<Integer, ChapterInfo> chapterMap, int textLength) {
        List<ChapterInfo> chapters = new ArrayList<>(chapterMap.values());
        chapters.sort(Comparator.comparingInt(ChapterInfo::getNumber));

        setEndPositions(chapters, textLength);
        chapters.forEach(this::finalizeLessons);

        return chapters;
    }

    private void setEndPositions(List<ChapterInfo> items, int defaultEnd) {
        for (int i = 0; i < items.size(); i++) {
            ChapterInfo current = items.get(i);
            current.setEndPosition(i + 1 < items.size() ? items.get(i + 1).getStartPosition() - 1 : defaultEnd);
        }
    }

    private void finalizeLessons(ChapterInfo chapter) {
        List<LessonInfo> lessons = chapter.getLessons();
        lessons.sort(Comparator.comparingInt(LessonInfo::getStartPosition));

        for (int i = 0; i < lessons.size(); i++) {
            LessonInfo current = lessons.get(i);
            current.setEndPosition(
                    i + 1 < lessons.size() ? lessons.get(i + 1).getStartPosition() - 1 : chapter.getEndPosition());
        }
    }

    private void logStructureSummary(List<ChapterInfo> chapters) {
        log.info("Created {} chapters with lessons:", chapters.size());
        chapters.forEach(chapter -> {
            log.info(
                    "  Chapter {}: {} ({} lessons)",
                    chapter.getNumber(),
                    chapter.getTitle(),
                    chapter.getLessons().size());
            chapter.getLessons()
                    .forEach(lesson -> log.info(
                            "    Lesson {}: {} (ID: {})", lesson.getNumber(), lesson.getTitle(), lesson.getLessonId()));
        });
    }

    private DocumentStructure buildStructureFromContent(String text) {
        DocumentStructure structure = new DocumentStructure();
        List<ChapterInfo> chapters = extractChapters(text);
        chapters.forEach(chapter -> chapter.setLessons(extractLessonsInRange(text, chapter)));
        structure.setChapters(chapters);
        return structure;
    }

    private List<ChapterInfo> extractChapters(String text) {
        List<ChapterInfo> chapters = new ArrayList<>();
        Matcher matcher = CHAPTER_PATTERN.matcher(text);

        while (matcher.find()) {
            ChapterInfo chapter = new ChapterInfo();
            chapter.setNumber(Integer.parseInt(matcher.group(1)));
            chapter.setTitle(
                    "Chương " + chapter.getNumber() + " " + matcher.group(2).trim());
            chapter.setStartPosition(matcher.start());
            chapters.add(chapter);
        }

        setEndPositions(chapters, text.length());
        return chapters;
    }

    private List<LessonInfo> extractLessonsInRange(String text, ChapterInfo chapter) {
        List<LessonInfo> lessons = new ArrayList<>();
        String chapterText = text.substring(chapter.getStartPosition(), chapter.getEndPosition());
        Matcher matcher = LESSON_PATTERN.matcher(chapterText);

        while (matcher.find()) {
            LessonInfo lesson = new LessonInfo();
            lesson.setNumber(Integer.parseInt(matcher.group(1)));
            lesson.setTitle("Bài " + lesson.getNumber() + " " + matcher.group(2).trim());
            lesson.setStartPosition(chapter.getStartPosition() + matcher.start());
            lesson.setChapterNumber(chapter.getNumber());
            lesson.setLessonId(generateLessonId(chapter.getNumber(), lesson.getNumber()));
            lessons.add(lesson);
        }

        for (int i = 0; i < lessons.size(); i++) {
            LessonInfo current = lessons.get(i);
            current.setEndPosition(
                    i + 1 < lessons.size() ? lessons.get(i + 1).getStartPosition() : chapter.getEndPosition());
        }

        return lessons;
    }

    public StructureContext findStructureContext(DocumentStructure structure, int textPosition, int pageNumber) {
        if (tocPageMapping != null && !tocPageMapping.isEmpty() && pageNumber > 0) {
            return findStructureContextByPage(pageNumber);
        }
        return findStructureContextByPosition(structure, textPosition);
    }

    private StructureContext findStructureContextByPage(int pageNumber) {
        StructureContext context = new StructureContext();
        PageMapping mapping = findPageMapping(pageNumber);

        if (mapping != null) {
            if (mapping.getChapterNumber() != null) {
                ChapterInfo chapter = new ChapterInfo();
                chapter.setNumber(mapping.getChapterNumber());
                chapter.setTitle(mapping.getChapterTitle());
                context.setChapter(chapter);
            }

            if (mapping.getLessonNumber() != null) {
                LessonInfo lesson = new LessonInfo();
                lesson.setNumber(mapping.getLessonNumber());
                lesson.setTitle(mapping.getLessonTitle());
                lesson.setLessonId(mapping.getLessonId());
                lesson.setChapterNumber(mapping.getChapterNumber() != null ? mapping.getChapterNumber() : 0);
                context.setLesson(lesson);
            }
        }

        return context;
    }

    private PageMapping findPageMapping(int pageNumber) {
        if (tocPageMapping == null) return null;

        PageMapping mapping = tocPageMapping.get(pageNumber);
        if (mapping != null) return mapping;

        for (int offset = 1; offset <= PAGE_OFFSET_RANGE; offset++) {
            mapping = tocPageMapping.get(pageNumber - offset);
            if (mapping != null) return mapping;

            mapping = tocPageMapping.get(pageNumber + offset);
            if (mapping != null) return mapping;
        }

        return null;
    }

    private StructureContext findStructureContextByPosition(DocumentStructure structure, int textPosition) {
        StructureContext context = new StructureContext();

        for (ChapterInfo chapter : structure.getChapters()) {
            if (textPosition >= chapter.getStartPosition() && textPosition < chapter.getEndPosition()) {
                context.setChapter(chapter);

                for (LessonInfo lesson : chapter.getLessons()) {
                    if (textPosition >= lesson.getStartPosition() && textPosition < lesson.getEndPosition()) {
                        context.setLesson(lesson);
                        break;
                    }
                }
                break;
            }
        }

        return context;
    }

    @Override
    public StructureContext findStructureContextWithContentAnalysis(
            DocumentStructure structure, int textPosition, int estimatedPage, String chunkContent) {

        log.info(
                "Finding context for position {}, page {}, content: '{}'",
                textPosition,
                estimatedPage,
                chunkContent.substring(0, Math.min(100, chunkContent.length())));

        // Priority 1: Content analysis
        StructureContext context = findStructureContextByContent(chunkContent, structure);
        if (context.hasLesson()) {
            log.info(
                    "✓ Found via CONTENT: Chapter {}, Lesson {} '{}'",
                    context.getChapter().getNumber(),
                    context.getLesson().getNumber(),
                    context.getLesson().getTitle());
            return context;
        }

        // Priority 2: Page mapping
        if (tocPageMapping != null && !tocPageMapping.isEmpty() && estimatedPage > 0) {
            context = findStructureContextByPage(estimatedPage);
            if (context.hasLesson()) {
                log.info(
                        "✓ Found via PAGE {}: Chapter {}, Lesson {} '{}'",
                        estimatedPage,
                        context.getChapter().getNumber(),
                        context.getLesson().getNumber(),
                        context.getLesson().getTitle());
                return context;
            }
        }

        // Priority 3: Position
        context = findStructureContextByPosition(structure, textPosition);
        if (context.hasLesson()) {
            log.info(
                    "✓ Found via POSITION {}: Chapter {}, Lesson {} '{}'",
                    textPosition,
                    context.getChapter().getNumber(),
                    context.getLesson().getNumber(),
                    context.getLesson().getTitle());
        } else {
            log.warn("❌ No context found for position {}, page {}", textPosition, estimatedPage);
        }

        return context;
    }

    private StructureContext findStructureContextByContent(String chunkContent, DocumentStructure structure) {
        if (chunkContent == null || chunkContent.trim().isEmpty()) {
            return new StructureContext();
        }

        String contentLower = chunkContent.toLowerCase();
        Map<LessonInfo, Integer> lessonScores = calculateLessonScores(contentLower, structure);

        return findBestMatch(lessonScores, structure, chunkContent);
    }

    private Map<LessonInfo, Integer> calculateLessonScores(String contentLower, DocumentStructure structure) {
        Map<LessonInfo, Integer> scores = new HashMap<>();

        for (ChapterInfo chapter : structure.getChapters()) {
            for (LessonInfo lesson : chapter.getLessons()) {
                int score = calculateLessonScore(contentLower, lesson, chapter);
                if (score > 0) {
                    scores.put(lesson, score);
                }
            }
        }

        return scores;
    }

    private int calculateLessonScore(String contentLower, LessonInfo lesson, ChapterInfo chapter) {
        String title = lesson.getTitle().toLowerCase();
        int score = 0;

        // Exact title match
        if (contentLower.contains(title)) {
            score += 100;
        }

        // Key phrases
        String[] titleWords = title.split("\\s+");
        if (titleWords.length >= 2) {
            for (int i = 0; i < titleWords.length - 1; i++) {
                String phrase = titleWords[i] + " " + titleWords[i + 1];
                if (phrase.length() > 4 && contentLower.contains(phrase)) {
                    score += 50;
                }
            }
        }

        // Individual words
        for (String word : titleWords) {
            if (word.length() > 3) {
                score += countOccurrences(contentLower, word) * 3;
            }
        }

        // Special content patterns
        score += getSpecialContentScore(contentLower, lesson);

        // Context consistency
        if (chapter.getNumber() == 1 && containsChapter1Indicators(contentLower)) {
            score += 20;
        }
        if (chapter.getNumber() == 2 && !containsChapter2StrongIndicators(contentLower)) {
            score -= 30;
        }

        return score;
    }

    private StructureContext findBestMatch(
            Map<LessonInfo, Integer> scores, DocumentStructure structure, String chunkContent) {
        StructureContext context = new StructureContext();

        LessonInfo bestMatch = null;
        int bestScore = 0;

        for (Map.Entry<LessonInfo, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestMatch = entry.getKey();
            }
        }

        if (bestMatch != null && bestScore >= MIN_SCORE_THRESHOLD) {
            context.setLesson(bestMatch);
            LessonInfo finalBestMatch = bestMatch;
            structure.getChapters().stream()
                    .filter(ch -> ch.getLessons().contains(finalBestMatch))
                    .findFirst()
                    .ifPresent(context::setChapter);

            log.info(
                    "Content matched: '{}' -> Lesson {} (score: {})",
                    chunkContent.substring(0, Math.min(50, chunkContent.length())),
                    bestMatch.getNumber(),
                    bestScore);
        }

        return context;
    }

    private int countOccurrences(String text, String keyword) {
        if (keyword.length() < 3) return 0;
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    private boolean containsChapter1Indicators(String contentLower) {
        return contentLower.contains("số tự nhiên")
                || contentLower.contains("tập hợp số tự nhiên")
                || contentLower.contains("thực hành")
                || contentLower.contains("bài tập")
                || (contentLower.contains("phép tính") && contentLower.contains("tập hợp"));
    }

    private boolean containsChapter2StrongIndicators(String contentLower) {
        return contentLower.contains("số nguyên")
                || contentLower.contains("số nguyên âm")
                || contentLower.contains("tập hợp số nguyên")
                || contentLower.contains("hai số nguyên");
    }

    private int getSpecialContentScore(String contentLower, LessonInfo lesson) {
        int score = 0;

        switch (lesson.getNumber()) {
            case 6:
                if (contentLower.contains("chia hết") && contentLower.contains("chia có dư")) score += 80;
                if (contentLower.contains("thương") && contentLower.contains("số dư")) score += 60;
                if (contentLower.contains("tính chất chia hết")) score += 70;
                break;
            case 12:
                if (contentLower.contains("ước chung lớn nhất") || contentLower.contains("ước chung lớn")) score += 80;
                if (contentLower.contains("ước chung") && !contentLower.contains("chia hết")) score += 40;
                break;
            case 7:
                if (contentLower.contains("dấu hiệu chia hết")) score += 70;
                if (contentLower.contains("chia hết cho 2") || contentLower.contains("chia hết cho 5")) score += 60;
                break;
            case 8:
                if (contentLower.contains("chia hết cho 3") || contentLower.contains("chia hết cho 9")) score += 60;
                break;
            case 2:
                if (contentLower.contains("tập hợp số tự nhiên")) score += 70;
                if (contentLower.contains("ghi số tự nhiên")) score += 60;
                break;
            case 3:
                if (contentLower.contains("phép tính") && contentLower.contains("tập hợp số tự nhiên")) score += 70;
                if (contentLower.contains("chia hết") || contentLower.contains("chia có dư")) score -= 50;
                break;
            case 4:
                if (contentLower.contains("luỹ thừa")) score += 80;
                if (contentLower.contains("số mũ")) score += 60;
                if (contentLower.contains("cơ số")) score += 50;
                break;
        }

        return score;
    }

    private String generateLessonId(int chapterNumber, int lessonNumber) {
        return String.format("chapter_%d_lesson_%d", chapterNumber, lessonNumber);
    }

    private int estimatePositionFromPage(int pageNumber) {
        return Math.max(0, (pageNumber - 1) * AVG_CHARS_PER_PAGE);
    }

    private int estimateTotalPages(String text) {
        return text == null || text.isEmpty() ? 1 : Math.max(1, text.length() / AVG_CHARS_PER_PAGE);
    }

    public Map<Integer, PageMapping> getTocPageMapping() {
        return tocPageMapping;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
