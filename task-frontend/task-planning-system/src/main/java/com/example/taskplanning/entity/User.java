package com.example.taskplanning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Set; // 记得在文件顶部导入
import java.util.HashSet;

@Getter
@Setter
@Entity
@Table(name = "users") // 明确指定表名为 users (复数)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name="name",nullable = false)
    private String name;

    @Column(name="avatarUrl",nullable = true)
    private String avatarUrl;

    @Column(name="phone",nullable = false)
    private String phone;

    @Column(name="language",nullable = true)
    private String language="zh-CN";

    @Column(name="emailVerified",nullable = false)
    private boolean emailVerified;

    @Column(name="lastLoginAt",nullable = true)
    private LocalDateTime lastLoginAt;

    @Column(name="notificationSettings",nullable = false)
    private boolean notificationSettings;

    @Column(name="verification_token")
    private String verificationToken;

    @Column(name="verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "created_at", updatable = false) // 创建后不可更新
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
// mappedBy = "owner" 指的是，这个一对多关系是由ClassEntity实体中的'owner'字段来维护的。
// cascade = CascadeType.ALL 表示级联操作，比如删除用户时，他创建的班级也会被一并删除。
// orphanRemoval = true 表示移除孤儿数据。
    private Set<Classes> createdClasses = new HashSet<>();


    // 使用 @PrePersist 和 @PreUpdate 注解自动管理时间戳
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        // 注册时，将displayName默认设置为username
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    // ...其他属性...
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserClassRelation> classRelations = new HashSet<>();
    // ...其他属性...
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserTaskRelation> taskRelations = new HashSet<>();
}