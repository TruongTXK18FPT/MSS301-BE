package com.mss301.mindmapservice.service;

import java.util.List;

import com.mss301.mindmapservice.dto.request.ClassroomMindmapRequest;
import com.mss301.mindmapservice.dto.response.ClassroomMindmapResponse;

public interface ClassroomMindmapService {

    /**
     * Share a mindmap to a classroom (Teacher only)
     */
    ClassroomMindmapResponse shareMindmapToClassroom(ClassroomMindmapRequest request, Long teacherId);

    /**
     * Remove a mindmap from a classroom
     */
    void removeMindmapFromClassroom(Long classroomMindmapId, Long teacherId);

    /**
     * Get all mindmaps shared to a classroom
     */
    List<ClassroomMindmapResponse> getMindmapsByClassroom(Long classroomId);

    /**
     * Get all classrooms a mindmap is shared to
     */
    List<ClassroomMindmapResponse> getClassroomsByMindmap(Long mindmapId);

    /**
     * Get all mindmaps shared by a teacher
     */
    List<ClassroomMindmapResponse> getMindmapsByTeacher(Long teacherId);
}
