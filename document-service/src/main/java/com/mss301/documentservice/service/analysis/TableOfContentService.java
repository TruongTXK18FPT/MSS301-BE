package com.mss301.documentservice.service.analysis;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.mss301.documentservice.service.analysis.models.toc.PageMapping;
import com.mss301.documentservice.service.analysis.models.toc.TocEntry;

@Service
public interface TableOfContentService {
    public List<TocEntry> parseTableOfContents(String fullText);

    public Map<Integer, PageMapping> createPageMapping(List<TocEntry> tocEntries, int totalPages);
}
