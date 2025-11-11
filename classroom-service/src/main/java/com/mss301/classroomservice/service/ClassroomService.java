package com.mss301.classroomservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mss301.classroomservice.dto.request.ClassroomRequest;
import com.mss301.classroomservice.dto.response.ClassroomResponse;
import com.mss301.classroomservice.dto.response.ClassroomSummaryResponse;
import com.mss301.classroomservice.dto.response.StudentResponse;

public interface ClassroomService {
    ClassroomResponse create(ClassroomRequest request, Long ownerId);

    ClassroomResponse update(Long id, ClassroomRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    ClassroomResponse getById(Long id, Long userId);

    List<ClassroomResponse> getMyClassrooms(Long userId); // Get classrooms where user is owner or member

    List<ClassroomResponse> getPublicClassrooms();

    Page<ClassroomResponse> getAllClassrooms(Pageable pageable); // Get all classrooms with pagination (admin)

    String generateJoinCode(Long id, Long ownerId);

    ClassroomResponse joinByCode(String joinCode, Long userId);
    
    // Tìm kiếm lớp học cho học sinh
    List<ClassroomResponse> searchClassrooms(String keyword);
    
    // Tham gia lớp học bằng mật khẩu
    ClassroomResponse joinClassroom(String classroomCode, String password, Long userId);
    
    // Lấy danh sách học sinh trong lớp
    List<StudentResponse> getClassroomStudents(Long classroomId, Long teacherId);
    
    // Xóa học sinh khỏi lớp
    void removeStudentFromClassroom(Long classroomId, Long studentId, Long teacherId);
    
    // Lấy tổng quan chi tiết về lớp học (cho teacher và student)
    ClassroomSummaryResponse getClassroomSummary(Long classroomId, Long userId);
}
