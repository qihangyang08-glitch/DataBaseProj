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
@Table(name = "classes") // 明确表名
public class Classes { // 使用ClassEntity避免与Java关键字'class'冲突

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Lob // @Lob注解表示这是一个大的文本字段，对应数据库的TEXT类型
    private String description;

    @Column(name = "invite_code", nullable = false, unique = true, length = 20)
    private String inviteCode;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false; // 默认不公开

    @Column(name = "join_approval", nullable = false)
    private boolean joinApprovalRequired = true; // 默认需要审批

    @Enumerated(EnumType.STRING) // 将枚举类型以字符串形式存入数据库，可读性更好
    @Column(nullable = false, length = 20)
    private ClassStatus status = ClassStatus.ACTIVE; // 默认状态为活跃

    // --- 关系与外键定义 ---

    @ManyToOne(fetch = FetchType.LAZY) // 使用LAZY fetch, 性能更优
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // 班级的创建者，直接引用User实体

    // --- 自动管理的时间戳 ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- 定义与User的多对多关系的反向端 (可选但推荐) ---
    /*
    @ManyToMany(mappedBy = "joinedClasses")
    private Set<User> members = new HashSet<>();
    */
    // 注意：由于我们的中间表有额外字段，所以不会直接用@ManyToMany，
    // 我们之后会通过UserClassRelation实体来管理这个关系。所以这里先注释掉。


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

    // 定义一个枚举来表示班级状态，比用字符串或数字更安全、更清晰
    public enum ClassStatus {
        ACTIVE,
        ARCHIVED
    }
    // ...其他属性...
    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserClassRelation> memberRelations = new HashSet<>();
}
