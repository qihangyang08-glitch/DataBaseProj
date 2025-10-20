// 2. ClassJoinRequestDto.java
package com.example.taskplanning.dto;

import jakarta.validation.constraints.Size;

public class ClassJoinRequestDto {

    @Size(max = 500, message = "申请理由长度不能超过500个字符")
    private String joinReason;

    // 构造函数
    public ClassJoinRequestDto() {
    }

    public ClassJoinRequestDto(String joinReason) {
        this.joinReason = joinReason;
    }

    // Getters and Setters
    public String getJoinReason() {
        return joinReason;
    }

    public void setJoinReason(String joinReason) {
        this.joinReason = joinReason;
    }
}
