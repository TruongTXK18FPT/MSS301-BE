package com.mss301.documentservice.service.analysis.models.structure;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DocumentStructure {
    private List<ChapterInfo> chapters = new ArrayList<>();
}
