package com.example.taskplanning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    private String description;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType; // 任务类型 (个人/班级)

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; // 软删除标记，默认为false

    private LocalDateTime deadline; // 截止时间


    // --- 关系与外键定义 ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator; // 创建者

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id") // 个人任务时，此字段可以为null
    private Classes classEntity; // 所属班级

    // --- 自动管理的时间戳 ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    // 定义任务类型的枚举
    public enum TaskType {
        PERSONAL,
        CLASS
    }
    // ...其他属性...
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserTaskRelation> userRelations = new HashSet<>();
}