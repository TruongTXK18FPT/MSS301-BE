package com.mss301.classroomservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomSummaryResponse {
    
    // Basic classroom info
    private Long id;
    private String name;
    private String description;
    private String joinCode;
    private String subject;
    private String grade;
    
    // Stats
    private ClassroomStats stats;
    
    // Students with their performance
    private List<StudentSummary> students;
    
    // Content items (mindmaps, lessons, assignments, quizzes)
    private List<ContentItemSummary> contentItems;
    
    // Recent activities
    private List<RecentActivity> recentActivities;
    
    // Upcoming deadlines
    private List<UpcomingDeadline> upcomingDeadlines;
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassroomStats {
        private int totalStudents;
        private int totalMindmaps;
        private int totalLessons;
        private int totalAssignments;
        private int totalQuizzes;
        private int pendingSubmissions;
        private int gradedSubmissions;
        private double averageScore;
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private Long userId;
        private String fullName;
        private String email;
        private LocalDateTime joinedAt;
        private int completedAssignments;
        private int completedQuizzes;
        private double averageScore;
        private String status; // ACTIVE, INACTIVE
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentItemSummary {
        private Long id;
        private String type; // LESSON, ASSIGNMENT, QUIZ, RESOURCE (mindmap)
        private Long contentId; // Reference to actual content
        private String title;
        private String description;
        private Boolean visible;
        private LocalDateTime publishAt;
        private LocalDateTime dueAt;
        private Integer maxPoints;
        private Integer submissionCount;
        private Integer viewCount;
        private LocalDateTime createdAt;
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type; // SUBMISSION, GRADE, JOIN, CONTENT_ADDED
        private Long userId;
        private String userName;
        private String action;
        private Long contentId;
        private String contentTitle;
        private LocalDateTime timestamp;
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingDeadline {
        private Long contentId;
        private String type; // ASSIGNMENT, QUIZ
        private String title;
        private LocalDateTime dueAt;
        private Integer submittedCount;
        private Integer totalStudents;
        private boolean isOverdue;
    }
}
