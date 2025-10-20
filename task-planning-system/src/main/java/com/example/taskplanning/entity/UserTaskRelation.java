package com.example.taskplanning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_task_relations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "task_id"})
})
public class UserTaskRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 代理主键

    // --- 关系定义 ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    // --- 关系属性 (用户的"个人看法") ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO; // 个人完成状态，默认为待办

    @Column(name = "personal_deadline")
    private LocalDateTime personalDeadline; // 个人设定的截止时间

    @Lob // @Lob注解表示这是一个大的文本字段，对应数据库的TEXT类型
    @Column(name = "personal_notes")
    private String personalNotes; // 个人备注

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 完成时间

    // --- 自动管理的时间戳 ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 关系创建时间 (即"导入"时间)

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 关系更新时间 (即修改个人状态的时间)


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

    // --- 枚举定义 ---

    public enum TaskStatus {
        TODO,       // 待办
        IN_PROGRESS,// 进行中
        DONE        // 已完成
    }
}