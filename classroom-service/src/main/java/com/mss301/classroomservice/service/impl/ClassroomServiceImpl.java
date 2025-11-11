package com.mss301.classroomservice.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.classroomservice.dto.request.ClassroomRequest;
import com.mss301.classroomservice.dto.response.ClassroomResponse;
import com.mss301.classroomservice.dto.response.ClassroomSummaryResponse;
import com.mss301.classroomservice.dto.response.StudentResponse;
import com.mss301.classroomservice.entity.Classroom;
import com.mss301.classroomservice.entity.ClassroomMember;
import com.mss301.classroomservice.entity.ClassroomMember.Role;
import com.mss301.classroomservice.repository.AssignmentRepository;
import com.mss301.classroomservice.repository.ClassroomContentRepository;
import com.mss301.classroomservice.repository.ClassroomMemberRepository;
import com.mss301.classroomservice.repository.ClassroomRepository;
import com.mss301.classroomservice.repository.GradeRepository;
import com.mss301.classroomservice.repository.QuizRepository;
import com.mss301.classroomservice.repository.SubmissionRepository;
import com.mss301.classroomservice.service.ClassroomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final ClassroomContentRepository classroomContentRepository;
    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;

    @Override
    @Transactional
    public ClassroomResponse create(ClassroomRequest request, Long ownerId) {
        // Generate joinCode if not provided
        String joinCode = request.getJoinCode();
        if (joinCode == null || joinCode.trim().isEmpty()) {
            joinCode = generateJoinCode();
            System.out.println("Auto-generated joinCode: " + joinCode);
        }
        
        Classroom classroom = Classroom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .password(request.getPassword())
                .joinCode(joinCode)
                .maxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 50)
                .subject(request.getSubject() != null ? request.getSubject() : "Toán học")
                .grade(request.getGrade())
                .ownerId(ownerId)
                .build();
        classroom = classroomRepository.save(classroom);

        classroomMemberRepository.save(ClassroomMember.builder()
                .classroomId(classroom.getId())
                .userId(ownerId)
                .role(Role.TEACHER)
                .build());

        System.out.println("Created classroom: " + classroom.getName() + " with joinCode: " + classroom.getJoinCode());
        return toResponse(classroom);
    }
    
    // Helper method to generate random join code
    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // Check if code already exists, regenerate if needed
        String generatedCode = code.toString();
        while (classroomRepository.findByJoinCodeIgnoreCase(generatedCode).isPresent()) {
            code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            generatedCode = code.toString();
        }
        
        return generatedCode;
    }

    @Override
    @Transactional
    public ClassroomResponse update(Long id, ClassroomRequest request, Long ownerId) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        // Skip ownership check if ownerId is null (admin context)
        // Admin endpoints should validate authorization at controller level
        if (ownerId != null && !classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        classroom.setName(request.getName());
        classroom.setDescription(request.getDescription());
        classroom.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        classroom.setPassword(request.getPassword());
        // Don't update joinCode during updates - only during creation
        if (request.getJoinCode() != null && !request.getJoinCode().trim().isEmpty()) {
            classroom.setJoinCode(request.getJoinCode());
        }
        classroom.setMaxStudents(
                request.getMaxStudents() != null ? request.getMaxStudents() : classroom.getMaxStudents());
        classroom.setSubject(request.getSubject() != null ? request.getSubject() : classroom.getSubject());
        classroom.setGrade(request.getGrade() != null ? request.getGrade() : classroom.getGrade());
        return toResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        // Skip ownership check if ownerId is null (admin context)
        if (ownerId != null && !classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        classroomRepository.delete(classroom);
    }

    @Override
    public ClassroomResponse getById(Long id, Long userId) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        // Simple access control: owner or public or member
        boolean isMember = classroomMemberRepository.findByClassroomIdAndUserId(id, userId).isPresent();
        if (!classroom.getOwnerId().equals(userId) && !Boolean.TRUE.equals(classroom.getIsPublic()) && !isMember) {
            throw new RuntimeException("Forbidden");
        }
        return toResponse(classroom);
    }

    @Override
    public List<ClassroomResponse> getMyClassrooms(Long userId) {
        // Get classrooms where user is owner
        List<Classroom> ownedClassrooms = classroomRepository.findByOwnerId(userId);
        
        // Get classrooms where user is a member (student)
        List<Long> memberClassroomIds = classroomMemberRepository.findByUserId(userId).stream()
                .map(ClassroomMember::getClassroomId)
                .collect(Collectors.toList());
        
        List<Classroom> memberClassrooms = classroomRepository.findAllById(memberClassroomIds);
        
        // Combine both lists and remove duplicates
        List<Classroom> allClassrooms = new java.util.ArrayList<>(ownedClassrooms);
        for (Classroom classroom : memberClassrooms) {
            if (!allClassrooms.contains(classroom)) {
                allClassrooms.add(classroom);
            }
        }
        
        return allClassrooms.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassroomResponse> getPublicClassrooms() {
        return classroomRepository.findByIsPublicTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ClassroomResponse> getAllClassrooms(Pageable pageable) {
        return classroomRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public String generateJoinCode(Long id, Long ownerId) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        if (!classroom.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }
        byte[] buf = new byte[6];
        new SecureRandom().nextBytes(buf);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        classroom.setJoinCode(code);
        classroomRepository.save(classroom);
        return code;
    }

    @Override
    @Transactional
    public ClassroomResponse joinByCode(String joinCode, Long userId) {
        Classroom classroom = classroomRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new RuntimeException("Invalid code"));
        classroomMemberRepository
                .findByClassroomIdAndUserId(classroom.getId(), userId)
                .ifPresent(cm -> {
                    throw new RuntimeException("Already joined");
                });
        classroomMemberRepository.save(ClassroomMember.builder()
                .classroomId(classroom.getId())
                .userId(userId)
                .role(Role.STUDENT)
                .build());
        return toResponse(classroom);
    }

    @Override
    public List<ClassroomResponse> searchClassrooms(String keyword) {
        return classroomRepository.findByIsPublicTrueAndNameContainingIgnoreCase(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassroomResponse joinClassroom(String classroomCode, String password, Long userId) {
        // Log for debugging
        System.out.println("Attempting to join classroom with code: " + classroomCode);
        
        // Use case-insensitive search
        Classroom classroom = classroomRepository.findByJoinCodeIgnoreCase(classroomCode)
                .orElseThrow(() -> {
                    System.out.println("No classroom found with joinCode: " + classroomCode);
                    return new RuntimeException("Invalid classroom code: " + classroomCode);
                });

        System.out.println("Found classroom: " + classroom.getName() + " (ID: " + classroom.getId() + ")");

        // Check password if classroom has one
        if (classroom.getPassword() != null && !classroom.getPassword().isEmpty() 
            && !classroom.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }

        // Check if already a member
        classroomMemberRepository.findByClassroomIdAndUserId(classroom.getId(), userId)
                .ifPresent(cm -> {
                    throw new RuntimeException("Already joined");
                });

        // Add as student
        classroomMemberRepository.save(ClassroomMember.builder()
                .classroomId(classroom.getId())
                .userId(userId)
                .role(Role.STUDENT)
                .build());

        System.out.println("User " + userId + " successfully joined classroom " + classroom.getId());
        return toResponse(classroom);
    }

    @Override
    public List<StudentResponse> getClassroomStudents(Long classroomId, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        return classroomMemberRepository.findByClassroomIdAndRole(classroomId, Role.STUDENT).stream()
                .map(this::toStudentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeStudentFromClassroom(Long classroomId, Long studentId, Long teacherId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        if (!classroom.getOwnerId().equals(teacherId)) {
            throw new RuntimeException("Forbidden");
        }

        ClassroomMember member = classroomMemberRepository.findByClassroomIdAndUserId(classroomId, studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        classroomMemberRepository.delete(member);
    }

    private ClassroomResponse toResponse(Classroom classroom) {
        // Count current students
        long currentStudents = classroomMemberRepository.countByClassroomId(classroom.getId());
        
        // Count assignments
        int assignmentCount = assignmentRepository.findByClassroomId(classroom.getId()).size();
        
        // Count quizzes
        int quizCount = quizRepository.findByClassroomId(classroom.getId()).size();
        
        // Count content items (lessons/mindmaps)
        int contentCount = classroomContentRepository.findByClassroomIdOrderByOrderIndexAsc(classroom.getId()).size();

        return ClassroomResponse.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .isPublic(classroom.getIsPublic())
                .joinCode(classroom.getJoinCode())
                .password(classroom.getPassword())
                .maxStudents(classroom.getMaxStudents())
                .currentStudents((int) currentStudents)
                .assignmentCount(assignmentCount)
                .quizCount(quizCount)
                .contentCount(contentCount)
                .ownerId(classroom.getOwnerId())
                .subject(classroom.getSubject())
                .grade(classroom.getGrade())
                .createdAt(classroom.getCreatedAt())
                .updatedAt(classroom.getUpdatedAt())
                .build();
    }

    private StudentResponse toStudentResponse(ClassroomMember member) {
        // In a real implementation, you would fetch user details from user service
        return StudentResponse.builder()
                .userId(member.getUserId())
                .email("user" + member.getUserId() + "@example.com") // Placeholder
                .fullName("Student " + member.getUserId()) // Placeholder
                .joinedAt(member.getJoinedAt())
                .role(member.getRole().name())
                .build();
    }

    @Override
    public ClassroomSummaryResponse getClassroomSummary(Long classroomId, Long userId) {
        // Verify classroom exists and user has access
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        
        boolean isMember = classroomMemberRepository.findByClassroomIdAndUserId(classroomId, userId).isPresent();
        boolean isOwner = classroom.getOwnerId().equals(userId);
        
        if (!isOwner && !Boolean.TRUE.equals(classroom.getIsPublic()) && !isMember) {
            throw new RuntimeException("Forbidden: You don't have access to this classroom");
        }

        // 1. Build Stats
        List<ClassroomMember> students = classroomMemberRepository.findByClassroomIdAndRole(classroomId, Role.STUDENT);
        List<com.mss301.classroomservice.entity.ClassroomContent> allContent = 
            classroomContentRepository.findByClassroomIdOrderByOrderIndexAsc(classroomId);
        
        long totalMindmaps = allContent.stream()
            .filter(c -> c.getType() == com.mss301.classroomservice.entity.ClassroomContent.ContentType.RESOURCE)
            .count();
        long totalLessons = allContent.stream()
            .filter(c -> c.getType() == com.mss301.classroomservice.entity.ClassroomContent.ContentType.LESSON)
            .count();
        long totalAssignments = allContent.stream()
            .filter(c -> c.getType() == com.mss301.classroomservice.entity.ClassroomContent.ContentType.ASSIGNMENT)
            .count();
        long totalQuizzes = allContent.stream()
            .filter(c -> c.getType() == com.mss301.classroomservice.entity.ClassroomContent.ContentType.QUIZ)
            .count();
        
        // Count submissions
        long totalSubmissions = allContent.stream()
            .mapToLong(c -> submissionRepository.countByClassroomContentId(c.getId()))
            .sum();
        
        // Count graded submissions
        long gradedSubmissions = submissionRepository.findAll().stream()
            .filter(s -> gradeRepository.findBySubmissionId(s.getId()).isPresent())
            .count();
        
        // Calculate average score
        Double avgScore = students.isEmpty() ? 0.0 : 
            students.stream()
                .mapToDouble(s -> {
                    Double score = gradeRepository.getAverageScoreByStudent(s.getUserId());
                    return score != null ? score : 0.0;
                })
                .average()
                .orElse(0.0);

        ClassroomSummaryResponse.ClassroomStats stats = ClassroomSummaryResponse.ClassroomStats.builder()
                .totalStudents(students.size())
                .totalMindmaps((int) totalMindmaps)
                .totalLessons((int) totalLessons)
                .totalAssignments((int) totalAssignments)
                .totalQuizzes((int) totalQuizzes)
                .pendingSubmissions((int) (totalSubmissions - gradedSubmissions))
                .gradedSubmissions((int) gradedSubmissions)
                .averageScore(avgScore)
                .build();

        // 2. Build Student Summaries
        List<ClassroomSummaryResponse.StudentSummary> studentSummaries = students.stream()
                .map(member -> {
                    List<com.mss301.classroomservice.entity.Submission> studentSubmissions = 
                        submissionRepository.findByStudentId(member.getUserId());
                    
                    long completedAssignments = studentSubmissions.stream()
                        .filter(s -> s.getType() == com.mss301.classroomservice.entity.Submission.SubmissionType.ASSIGNMENT)
                        .count();
                    
                    long completedQuizzes = studentSubmissions.stream()
                        .filter(s -> s.getType() == com.mss301.classroomservice.entity.Submission.SubmissionType.QUIZ)
                        .count();
                    
                    Double studentAvgScore = gradeRepository.getAverageScoreByStudent(member.getUserId());
                    
                    return ClassroomSummaryResponse.StudentSummary.builder()
                            .userId(member.getUserId())
                            .fullName("Student " + member.getUserId()) // TODO: Fetch from user service
                            .email("user" + member.getUserId() + "@example.com") // TODO: Fetch from user service
                            .joinedAt(member.getJoinedAt())
                            .completedAssignments((int) completedAssignments)
                            .completedQuizzes((int) completedQuizzes)
                            .averageScore(studentAvgScore != null ? studentAvgScore : 0.0)
                            .status("ACTIVE")
                            .build();
                })
                .collect(Collectors.toList());

        // 3. Build Content Item Summaries
        List<ClassroomSummaryResponse.ContentItemSummary> contentSummaries = allContent.stream()
                .map(content -> {
                    long submissionCount = submissionRepository.countByClassroomContentId(content.getId());
                    
                    return ClassroomSummaryResponse.ContentItemSummary.builder()
                            .id(content.getId())
                            .type(content.getType().name())
                            .contentId(content.getContentId())
                            .title("Content " + content.getContentId()) // TODO: Fetch actual title from content service
                            .description("")
                            .visible(content.getVisible())
                            .publishAt(content.getPublishAt())
                            .dueAt(content.getDueAt())
                            .maxPoints(content.getMaxPoints())
                            .submissionCount((int) submissionCount)
                            .viewCount(0) // TODO: Implement view tracking
                            .createdAt(content.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        // 4. Build Recent Activities (placeholder - would need activity logging)
        List<ClassroomSummaryResponse.RecentActivity> recentActivities = List.of();

        // 5. Build Upcoming Deadlines
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<ClassroomSummaryResponse.UpcomingDeadline> upcomingDeadlines = allContent.stream()
                .filter(c -> c.getDueAt() != null)
                .filter(c -> c.getType() == com.mss301.classroomservice.entity.ClassroomContent.ContentType.ASSIGNMENT ||
                            c.getType() == com.mss301.classroomservice.entity.ClassroomContent.ContentType.QUIZ)
                .map(content -> {
                    long submittedCount = submissionRepository.countByClassroomContentId(content.getId());
                    boolean isOverdue = content.getDueAt().isBefore(now);
                    
                    return ClassroomSummaryResponse.UpcomingDeadline.builder()
                            .contentId(content.getId())
                            .type(content.getType().name())
                            .title("Content " + content.getContentId())
                            .dueAt(content.getDueAt())
                            .submittedCount((int) submittedCount)
                            .totalStudents(students.size())
                            .isOverdue(isOverdue)
                            .build();
                })
                .sorted((a, b) -> a.getDueAt().compareTo(b.getDueAt()))
                .limit(10)
                .collect(Collectors.toList());

        // Build final response
        return ClassroomSummaryResponse.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .joinCode(classroom.getJoinCode())
                .subject(classroom.getSubject())
                .grade(classroom.getGrade())
                .stats(stats)
                .students(studentSummaries)
                .contentItems(contentSummaries)
                .recentActivities(recentActivities)
                .upcomingDeadlines(upcomingDeadlines)
                .build();
    }
}
