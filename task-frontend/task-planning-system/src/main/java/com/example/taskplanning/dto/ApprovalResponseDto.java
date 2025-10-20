// 5. ApprovalResponseDto.java
package com.example.taskplanning.dto;

import com.example.taskplanning.entity.User;
import com.example.taskplanning.entity.UserClassRelation;
import java.time.LocalDateTime;

public class ApprovalResponseDto {

    private Long userId;
    private String username;
    private String displayName;
    private String joinReason;
    private LocalDateTime createdAt;

    // 构造函数
    public ApprovalResponseDto() {
    }

    // 便利构造函数，用于从UserClassRelation转换
    public ApprovalResponseDto(UserClassRelation relation) {
        User user = relation.getUser();
        this.userId = user.getId();
        this.username = user.getUsername();
        this.displayName = user.getName();
        this.joinReason = relation.getJoinReason();
        this.createdAt = relation.getCreatedAt();
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getJoinReason() {
        return joinReason;
    }

    public void setJoinReason(String joinReason) {
        this.joinReason = joinReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}