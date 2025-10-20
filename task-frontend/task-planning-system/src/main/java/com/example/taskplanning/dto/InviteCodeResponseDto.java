package com.example.taskplanning.dto;

/**
 * 班级邀请码响应DTO
 */
public class InviteCodeResponseDto {

    private String inviteCode;

    // 无参构造
    public InviteCodeResponseDto() {
    }

    // 全参构造
    public InviteCodeResponseDto(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    // Getter and Setter
    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}