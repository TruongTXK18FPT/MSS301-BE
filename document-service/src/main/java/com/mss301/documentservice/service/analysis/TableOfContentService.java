package com.mss301.documentservice.service.analysis;

import java.util.List;
import java.util.Map;

import com.mss301.documentservice.service.analysis.models.toc.PageMapping;
import com.mss301.documentservice.service.analysis.models.toc.TocEntry;

public interface TableOfContentService {
    public List<TocEntry> parseTableOfContents(String fullText);

    public Map<Integer, PageMapping> createPageMapping(List<TocEntry> tocEntries, int totalPages);
}
