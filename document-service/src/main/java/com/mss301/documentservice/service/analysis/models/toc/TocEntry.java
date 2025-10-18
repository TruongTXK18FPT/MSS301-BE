package com.mss301.documentservice.service.analysis.models.toc;

import lombok.Data;

@Data
public class TocEntry {
    private String type; // "chapter" or "lesson"
    private int number;
    private String title;
    private int pageNumber;
    private Integer parentChapter; // For lessons

    public TocEntry(String type, int number, String title, int pageNumber) {
        this.type = type;
        this.number = number;
        this.title = title;
        this.pageNumber = pageNumber;
    }

    @Override
    public String toString() {
        return String.format("%s %d: %s (page %d)", type, number, title, pageNumber);
    }
}
