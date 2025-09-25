package com.mss301.profileservice.service;

/**
 * Main ProfileService interface that combines all profile-related services
 * This interface extends all the focused service interfaces to maintain
 * backward compatibility
 * while providing a cleaner architecture
 */
public interface ProfileService extends UserProfileService, EventProfileService, ProfileManagementService {
    // This interface inherits methods from:
    // - UserProfileService: getCurrentUserProfile, updateCurrentUserProfile
    // - EventProfileService: createProfileFromUserEvent, createTeacherProfile,
    // createGuardianProfile
    // - ProfileManagementService: createStudentProfile, getStudentProfileByUserId,
    // updateStudentProfile,
    // deleteStudentProfile, getAllStudentProfiles
}
