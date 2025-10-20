package com.example.taskplanning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
// 为 user_id 和 class_id 添加复合唯一约束，确保一个用户在一个班级里只有一条记录
@Table(name = "user_class_relations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "class_id"})
})
public class UserClassRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 使用代理主键，便于JPA操作

    // --- 关系定义 ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classes classEntity;

    // --- 关系属性 ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleInClass role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JoinStatus status;

    @Column(name = "join_reason", length = 500)
    private String joinReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by") // 审批人ID，可以为null
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    // --- 自动管理的时间戳 ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 记录创建（即申请）时间

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 记录关系最后更新时间


    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- 枚举定义 (放在类内部，结构更清晰) ---

    public enum RoleInClass {
        OWNER,
        ADMIN,
        MEMBER
    }

    public enum JoinStatus {
        PENDING,  // 待审批
        APPROVED, // 已批准
        REJECTED, // 已拒绝
        REMOVED   // 已移除 (被踢出或自己退出)
    }
}
