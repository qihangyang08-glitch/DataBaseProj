package com.example.taskplanning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "action_logs")
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 操作者，可以为null（系统操作）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String action; // e.g., "CREATE_CLASS", "USER_LOGIN"

    @Column(name = "entity_type", length = 50)
    private String entityType; // e.g., "CLASS", "TASK"

    @Column(name = "entity_id")
    private Long entityId;

    // 使用@Lob来存储可能很长的JSON字符串
    @Lob
    private String details; // 存储JSON格式的操作详情

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 为了方便创建日志，可以提供一个构造函数
    public ActionLog() {
    }

    public ActionLog(User user, String action, String entityType, Long entityId, String details, String ipAddress, String userAgent) {
        this.user = user;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}