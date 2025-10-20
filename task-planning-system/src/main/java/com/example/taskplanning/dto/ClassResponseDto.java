// 4. ClassResponseDto.java
package com.example.taskplanning.dto;

import com.example.taskplanning.entity.Classes;
import com.example.taskplanning.entity.User;
import java.time.LocalDateTime;

public class ClassResponseDto {

    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private boolean isPublic;
    private boolean joinApprovalRequired;
    private LocalDateTime createdAt;
    private UserSimpleDto owner;

    // 构造函数
    public ClassResponseDto() {
    }

    // 便利构造函数，用于从Entity转换
    public ClassResponseDto(Classes classEntity, User owner) {
        this.id = classEntity.getId();
        this.name = classEntity.getName();
        this.description = classEntity.getDescription();
        this.inviteCode = classEntity.getInviteCode();
        this.isPublic = classEntity.isPublic();
        this.joinApprovalRequired = classEntity.isJoinApprovalRequired();
        this.createdAt = classEntity.getCreatedAt();
        this.owner = new UserSimpleDto(owner.getId(), owner.getUsername(), owner.getName());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isJoinApprovalRequired() {
        return joinApprovalRequired;
    }

    public void setJoinApprovalRequired(boolean joinApprovalRequired) {
        this.joinApprovalRequired = joinApprovalRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserSimpleDto getOwner() {
        return owner;
    }

    public void setOwner(UserSimpleDto owner) {
        this.owner = owner;
    }

    // 内部类：简化的用户信息DTO
    public static class UserSimpleDto {
        private Long id;
        private String username;
        private String displayName;

        public UserSimpleDto() {
        }

        public UserSimpleDto(Long id, String username, String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}