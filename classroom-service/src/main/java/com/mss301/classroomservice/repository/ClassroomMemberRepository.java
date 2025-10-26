package com.mss301.classroomservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.classroomservice.entity.ClassroomMember;
import com.mss301.classroomservice.entity.ClassroomMember.Role;

@Repository
public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {
    List<ClassroomMember> findByClassroomId(Long classroomId);

    List<ClassroomMember> findByUserId(Long userId);

    Optional<ClassroomMember> findByClassroomIdAndUserId(Long classroomId, Long userId);

    long countByClassroomId(Long classroomId);
    
    List<ClassroomMember> findByClassroomIdAndRole(Long classroomId, Role role);
}
