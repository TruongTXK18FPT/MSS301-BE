package com.mss301.documentservice.service.analysis.models.structure;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DocumentStructure {
    private List<ChapterInfo> chapters = new ArrayList<>();
}
