package com.example.taskplanning.dto;

import com.example.taskplanning.entity.Classes;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.entity.UserClassRelation;
import java.time.LocalDateTime;

/**
 * 全局待审批申请的响应DTO
 * 聚合了申请人信息和班级信息
 */
public class GlobalApprovalResponseDto {

    private Long id; // UserClassRelation的ID
    private ApplicantDto applicant;
    private ClassInfoDto classInfo;
    private String joinReason;
    private UserClassRelation.JoinStatus status;
    private LocalDateTime createdAt;

    // 便利构造函数，用于从实体转换
    public GlobalApprovalResponseDto(UserClassRelation relation) {
        this.id = relation.getId();
        this.applicant = new ApplicantDto(relation.getUser());
        this.classInfo = new ClassInfoDto(relation.getClassEntity());
        this.joinReason = relation.getJoinReason();
        this.status = relation.getStatus();
        this.createdAt = relation.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ApplicantDto getApplicant() { return applicant; }
    public void setApplicant(ApplicantDto applicant) { this.applicant = applicant; }
    public ClassInfoDto getClassInfo() { return classInfo; }
    public void setClassInfo(ClassInfoDto classInfo) { this.classInfo = classInfo; }
    public String getJoinReason() { return joinReason; }
    public void setJoinReason(String joinReason) { this.joinReason = joinReason; }
    public UserClassRelation.JoinStatus getStatus() { return status; }
    public void setStatus(UserClassRelation.JoinStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * 申请人信息子DTO
     */
    public static class ApplicantDto {
        private Long userId;
        private String username;
        private String displayName;

        public ApplicantDto(User user) {
            this.userId = user.getId();
            this.username = user.getUsername();
            this.displayName = user.getName();
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    /**
     * 班级信息子DTO
     */
    public static class ClassInfoDto {
        private Long id;
        private String name;

        public ClassInfoDto(Classes classEntity) {
            this.id = classEntity.getId();
            this.name = classEntity.getName();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
