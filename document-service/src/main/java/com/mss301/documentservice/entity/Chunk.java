package com.mss301.documentservice.entity;

import com.mss301.documentservice.entity.embedded.DocumentStructure;
import com.mss301.documentservice.entity.embedded.ProcessingInfo;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Document(indexName = "chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chunk {
    @Id
    private String id;

    private String documentId;
    private Integer chunkIndex;

    @Field(type = FieldType.Object)
    private DocumentStructure structure;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Text)
    private String summary; // Optional ( Using AI to summarize, now tốn token quá =))

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private List<Float> embedding;

    @Field(type = FieldType.Object)
    private ProcessingInfo processingInfo;
}
