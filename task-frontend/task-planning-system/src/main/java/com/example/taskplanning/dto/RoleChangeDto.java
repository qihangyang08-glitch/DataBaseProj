package com.example.taskplanning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleChangeDto {

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(ADMIN|MEMBER)$", message = "角色只能是 ADMIN 或 MEMBER")
    private String role;

    public RoleChangeDto() {}

    public RoleChangeDto(String role) {
        this.role = role;
    }
}