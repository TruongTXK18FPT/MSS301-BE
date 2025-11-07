package com.mss301.mindmapservice.dto.request;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.mss301.mindmapservice.entity.MindmapNode.NodeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeTypeDeserializer extends JsonDeserializer<NodeType> {

    @Override
    public NodeType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            return NodeType.CONCEPT;
        }
        
        try {
            // Try direct enum match (case-insensitive)
            return NodeType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // Try to map common variations
            String upperValue = value.toUpperCase().trim();
            switch (upperValue) {
                case "CONCEPT", "KHÁI NIỆM":
                    return NodeType.CONCEPT;
                case "FORMULA", "CÔNG THỨC":
                    return NodeType.FORMULA;
                case "EXERCISE", "BÀI TẬP":
                    return NodeType.EXERCISE;
                case "EXAMPLE", "VÍ DỤ":
                    return NodeType.EXAMPLE;
                case "ROOT", "GỐC":
                    return NodeType.ROOT;
                case "TOPIC", "CHỦ ĐỀ":
                    return NodeType.TOPIC;
                case "SUBTOPIC", "CHỦ ĐỀ CON":
                    return NodeType.SUBTOPIC;
                case "DEFINITION", "ĐỊNH NGHĨA":
                    return NodeType.DEFINITION;
                case "THEOREM", "ĐỊNH LÝ":
                    return NodeType.THEOREM;
                case "PROOF", "CHỨNG MINH":
                    return NodeType.PROOF;
                default:
                    // Log warning and default to CONCEPT
                    log.warn("Unknown NodeType: {}, defaulting to CONCEPT", value);
                    return NodeType.CONCEPT;
            }
        }
    }
}

