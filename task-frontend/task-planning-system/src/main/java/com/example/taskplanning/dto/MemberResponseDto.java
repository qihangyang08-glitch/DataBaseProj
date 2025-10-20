package com.example.taskplanning.dto;

import com.example.taskplanning.entity.UserClassRelation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberResponseDto {
    private Long userId;
    private String username;
    private String displayName;
    private UserClassRelation.RoleInClass role; // 直接使用枚举类型

    public MemberResponseDto(UserClassRelation relation) {
        this.userId = relation.getUser().getId();
        this.username = relation.getUser().getUsername();
        this.displayName = relation.getUser().getName();
        this.role = relation.getRole();
    }
}

