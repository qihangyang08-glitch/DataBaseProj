// 1. ClassCreateDto.java
package com.example.taskplanning.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClassCreateDto {

    @NotBlank(message = "班级名称不能为空")
    @Size(min = 1, max = 100, message = "班级名称长度必须在1-100个字符之间")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    private boolean isPublic = true;

    private boolean joinApprovalRequired = false;

    // 构造函数
    public ClassCreateDto() {
    }

    public ClassCreateDto(String name, String description, boolean isPublic, boolean joinApprovalRequired) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.joinApprovalRequired = joinApprovalRequired;
    }

    // Getters and Setters
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
}
