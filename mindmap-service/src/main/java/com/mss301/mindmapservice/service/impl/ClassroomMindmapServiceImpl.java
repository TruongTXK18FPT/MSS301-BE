package com.mss301.mindmapservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.ClassroomMindmapRequest;
import com.mss301.mindmapservice.dto.response.ClassroomMindmapResponse;
import com.mss301.mindmapservice.entity.ClassroomMindmap;
import com.mss301.mindmapservice.entity.Mindmap;
import com.mss301.mindmapservice.repository.ClassroomMindmapRepository;
import com.mss301.mindmapservice.repository.MindmapRepository;
import com.mss301.mindmapservice.service.ClassroomMindmapService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomMindmapServiceImpl implements ClassroomMindmapService {

    private final ClassroomMindmapRepository classroomMindmapRepository;
    private final MindmapRepository mindmapRepository;

    @Override
    @Transactional
    public ClassroomMindmapResponse shareMindmapToClassroom(ClassroomMindmapRequest request, Long teacherId) {
        log.info("Sharing mindmap {} to classroom {} by teacher {}", 
                request.getMindmapId(), request.getClassroomId(), teacherId);

        // Verify mindmap exists and belongs to teacher or is already public
        Mindmap mindmap = mindmapRepository.findById(request.getMindmapId())
                .orElseThrow(() -> new RuntimeException("Mindmap not found with id: " + request.getMindmapId()));

        // Check if already shared
        classroomMindmapRepository.findByMindmapIdAndClassroomIdAndIsActiveTrue(
                request.getMindmapId(), request.getClassroomId()
        ).ifPresent(existing -> {
            throw new RuntimeException("Mindmap already shared to this classroom");
        });

        ClassroomMindmap classroomMindmap = new ClassroomMindmap();
        classroomMindmap.setMindmapId(request.getMindmapId());
        classroomMindmap.setClassroomId(request.getClassroomId());
        classroomMindmap.setTeacherId(teacherId);
        classroomMindmap.setIsActive(true);
        classroomMindmap.setExpiresAt(request.getExpiresAt());

        ClassroomMindmap saved = classroomMindmapRepository.save(classroomMindmap);
        
        // Update mindmap visibility
        mindmap.setVisibility(Mindmap.Visibility.CLASSROOM);
        mindmapRepository.save(mindmap);

        log.info("Mindmap shared to classroom successfully");

        return mapToResponse(saved, mindmap);
    }

    @Override
    @Transactional
    public void removeMindmapFromClassroom(Long classroomMindmapId, Long teacherId) {
        log.info("Removing classroom mindmap {} by teacher {}", classroomMindmapId, teacherId);

        ClassroomMindmap classroomMindmap = classroomMindmapRepository.findById(classroomMindmapId)
                .orElseThrow(() -> new RuntimeException("Classroom mindmap not found with id: " + classroomMindmapId));

        // Verify teacher owns this share
        if (!classroomMindmap.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized: You can only remove your own shared mindmaps");
        }

        classroomMindmap.setIsActive(false);
        classroomMindmapRepository.save(classroomMindmap);

        log.info("Classroom mindmap removed successfully");
    }

    @Override
    public List<ClassroomMindmapResponse> getMindmapsByClassroom(Long classroomId) {
        log.info("Getting mindmaps for classroom: {}", classroomId);
        
        return classroomMindmapRepository.findByClassroomIdAndIsActiveTrue(classroomId).stream()
                .map(cm -> {
                    Mindmap mindmap = mindmapRepository.findById(cm.getMindmapId()).orElse(null);
                    return mapToResponse(cm, mindmap);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassroomMindmapResponse> getClassroomsByMindmap(Long mindmapId) {
        log.info("Getting classrooms for mindmap: {}", mindmapId);
        
        Mindmap mindmap = mindmapRepository.findById(mindmapId).orElse(null);
        
        return classroomMindmapRepository.findByMindmapIdAndIsActiveTrue(mindmapId).stream()
                .map(cm -> mapToResponse(cm, mindmap))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassroomMindmapResponse> getMindmapsByTeacher(Long teacherId) {
        log.info("Getting mindmaps shared by teacher: {}", teacherId);
        
        return classroomMindmapRepository.findByTeacherIdAndIsActiveTrue(teacherId).stream()
                .map(cm -> {
                    Mindmap mindmap = mindmapRepository.findById(cm.getMindmapId()).orElse(null);
                    return mapToResponse(cm, mindmap);
                })
                .collect(Collectors.toList());
    }

    private ClassroomMindmapResponse mapToResponse(ClassroomMindmap classroomMindmap, Mindmap mindmap) {
        return ClassroomMindmapResponse.builder()
                .id(classroomMindmap.getId())
                .mindmapId(classroomMindmap.getMindmapId())
                .classroomId(classroomMindmap.getClassroomId())
                .teacherId(classroomMindmap.getTeacherId())
                .isActive(classroomMindmap.getIsActive())
                .sharedAt(classroomMindmap.getSharedAt())
                .expiresAt(classroomMindmap.getExpiresAt())
                .mindmapTitle(mindmap != null ? mindmap.getTitle() : null)
                .mindmapDescription(mindmap != null ? mindmap.getDescription() : null)
                .build();
    }
}
