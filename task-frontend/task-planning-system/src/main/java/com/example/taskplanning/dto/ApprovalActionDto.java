// 3. ApprovalActionDto.java
package com.example.taskplanning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ApprovalActionDto {

    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "操作类型只能是APPROVE或REJECT")
    private String action;

    // 构造函数
    public ApprovalActionDto() {
    }

    public ApprovalActionDto(String action) {
        this.action = action;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}