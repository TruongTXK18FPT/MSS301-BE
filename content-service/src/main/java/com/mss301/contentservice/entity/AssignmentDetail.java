package com.mss301.contentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignment_details")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDetail {

    @Id
    private Long contentItemId; // shares id with ContentItem

    @Lob
    private String instructions;

    @Column(length = 20)
    private String submissionType; // TEXT, FILE, BOTH

    @Column(length = 500)
    private String attachmentFileIds; // comma-separated file ids for reference materials
}
