package com.mss301.profileservice.listener;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.mss301.profileservice.event.TeacherApprovalEvent;
import com.mss301.profileservice.service.EventProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Listener for TeacherApprovalEvent to update TeacherProfile approval status
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherApprovalListener {

    private final EventProfileService eventProfileService;

    @Bean
    public Consumer<TeacherApprovalEvent> teacherApproval() {
        return event -> {
            try {
                log.info("Received TeacherApprovalEvent: userId={}, status={}, reason={}",
                        event.getUserId(), event.getApprovalStatus(), event.getRejectionReason());

                eventProfileService.updateTeacherApprovalStatus(event);

                log.info("Successfully processed TeacherApprovalEvent for userId: {}", event.getUserId());
            } catch (Exception e) {
                log.error("Error processing TeacherApprovalEvent for userId {}: {}",
                        event.getUserId(), e.getMessage(), e);
            }
        };
    }
}
