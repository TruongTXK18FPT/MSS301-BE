package com.mss301.documentservice.service.analysis;

import com.mss301.documentservice.service.analysis.models.toc.PageMapping;
import com.mss301.documentservice.service.analysis.models.toc.TocEntry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface TableOfContentService {
    public List<TocEntry> parseTableOfContents(String fullText);
    public Map<Integer, PageMapping> createPageMapping(List<TocEntry> tocEntries, int totalPages);
}
