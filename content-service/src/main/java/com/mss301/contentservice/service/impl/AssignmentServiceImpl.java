package com.mss301.contentservice.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.contentservice.dto.request.AssignmentDetailRequest;
import com.mss301.contentservice.dto.response.AssignmentDetailResponse;
import com.mss301.contentservice.entity.AssignmentDetail;
import com.mss301.contentservice.entity.ContentItem;
import com.mss301.contentservice.entity.ContentItem.Type;
import com.mss301.contentservice.repository.AssignmentDetailRepository;
import com.mss301.contentservice.repository.ContentItemRepository;
import com.mss301.contentservice.service.AssignmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final ContentItemRepository contentItemRepository;
    private final AssignmentDetailRepository assignmentDetailRepository;

    @Override
    public AssignmentDetailResponse getAssignment(Long contentItemId, Long userId) {
        ContentItem item = contentItemRepository
                .findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        if (item.getType() != Type.ASSIGNMENT) {
            throw new RuntimeException("Not an assignment");
        }
        return assignmentDetailRepository
                .findById(contentItemId)
                .map(a -> AssignmentDetailResponse.builder()
                        .instructions(a.getInstructions())
                        .submissionType(a.getSubmissionType())
                        .attachmentFileIds(a.getAttachmentFileIds())
                        .build())
                .orElse(null);
    }

    @Override
    @Transactional
    public AssignmentDetailResponse putAssignment(Long contentItemId, AssignmentDetailRequest request, Long userId) {
        ContentItem item = contentItemRepository
                .findById(contentItemId)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        if (!item.getOwnerId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        if (item.getType() != Type.ASSIGNMENT) {
            throw new RuntimeException("Not an assignment");
        }
        assignmentDetailRepository.findById(contentItemId).ifPresent(assignmentDetailRepository::delete);
        AssignmentDetail detail = AssignmentDetail.builder()
                .contentItemId(contentItemId)
                .instructions(request.getInstructions())
                .submissionType(request.getSubmissionType())
                .attachmentFileIds(request.getAttachmentFileIds())
                .build();
        assignmentDetailRepository.save(detail);
        return getAssignment(contentItemId, userId);
    }
}
