// UserRolePermissionResponseDto.java
package com.example.taskplanning.dto;

import com.example.taskplanning.entity.UserClassRelation;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 用户在班级中的角色权限响应DTO
 * 用于前端基于角色的UI条件渲染
 */
public class UserRolePermissionResponseDto {

    @JsonProperty("isMember")
    private boolean isMember;

    @JsonProperty("role")
    private String role;

    @JsonProperty("isOwner")
    private boolean isOwner;

    @JsonProperty("isAdmin")
    private boolean isAdmin;

    @JsonProperty("canManageMembers")
    private boolean canManageMembers;

    @JsonProperty("canPublishTasks")
    private boolean canPublishTasks;

    @JsonProperty("canViewApprovals")
    private boolean canViewApprovals;

    @JsonProperty("canManageClass")
    private boolean canManageClass;

    @JsonProperty("joinedAt")
    private LocalDateTime joinedAt;

    // 构造函数 - 用于非成员情况
    public UserRolePermissionResponseDto() {
        this.isMember = false;
        this.role = null;
        this.isOwner = false;
        this.isAdmin = false;
        this.canManageMembers = false;
        this.canPublishTasks = false;
        this.canViewApprovals = false;
        this.canManageClass = false;
        this.joinedAt = null;
    }

    // 构造函数 - 用于成员情况
    public UserRolePermissionResponseDto(UserClassRelation relation) {
        this.isMember = true;
        this.role = relation.getRole().toString();
        this.isOwner = relation.getRole() == UserClassRelation.RoleInClass.OWNER;
        this.isAdmin = relation.getRole() == UserClassRelation.RoleInClass.ADMIN;
        this.joinedAt = relation.getJoinedAt();

        // 根据角色设置权限
        this.canManageMembers = this.isOwner;
        this.canPublishTasks = this.isOwner || this.isAdmin;
        this.canViewApprovals = this.isOwner || this.isAdmin;
        this.canManageClass = this.isOwner;
    }

    // Getters and Setters
    public boolean isMember() {
        return isMember;
    }

    public void setMember(boolean member) {
        isMember = member;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isCanManageMembers() {
        return canManageMembers;
    }

    public void setCanManageMembers(boolean canManageMembers) {
        this.canManageMembers = canManageMembers;
    }

    public boolean isCanPublishTasks() {
        return canPublishTasks;
    }

    public void setCanPublishTasks(boolean canPublishTasks) {
        this.canPublishTasks = canPublishTasks;
    }

    public boolean isCanViewApprovals() {
        return canViewApprovals;
    }

    public void setCanViewApprovals(boolean canViewApprovals) {
        this.canViewApprovals = canViewApprovals;
    }

    public boolean isCanManageClass() {
        return canManageClass;
    }

    public void setCanManageClass(boolean canManageClass) {
        this.canManageClass = canManageClass;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}