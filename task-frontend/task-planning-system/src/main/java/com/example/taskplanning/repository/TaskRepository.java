// 6. TaskRepository.java - 任务数据访问接口
package com.example.taskplanning.repository;

import com.example.taskplanning.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 查找指定班级的所有未删除任务
     */
    Page<Task> findByClassEntityIdAndIsDeletedFalseOrderByDeadlineAsc(Long classId, Pageable pageable);

    /**
     * 查找用户的个人任务
     */
    Page<Task> findByCreatorIdAndTaskTypeAndIsDeletedFalseOrderByDeadlineAsc(Long creatorId, Task.TaskType taskType, Pageable pageable);

    /**
     * 查找指定时间范围内用户相关的所有任务（用于日历视图）
     * 严格遵循"用户自主控制"原则：
     * 1. 用户的个人任务
     * 2. 用户已主动关联的班级任务（通过 user_task_relations 表）
     */
    @Query("""
    SELECT DISTINCT t FROM Task t 
    WHERE t.isDeleted = false 
    AND (
        (t.taskType = 'PERSONAL' AND t.creator.id = :userId)
        OR 
        (t.taskType = 'CLASS' AND EXISTS (
            SELECT 1 FROM UserTaskRelation utr 
            WHERE utr.task.id = t.id AND utr.user.id = :userId
        ))
    )
    AND (
        (t.createdAt BETWEEN :startDate AND :endDate)
        OR 
        (t.deadline BETWEEN :startDate AND :endDate)
    )
    ORDER BY t.deadline ASC
""")
    List<Task> findUserRelatedTasksInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    /**
     * 新增：专门获取用户已关联的班级任务（供智能同步使用）
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 用户已关联的班级任务列表
     */
    @Query("""
    SELECT DISTINCT t FROM Task t 
    JOIN UserTaskRelation utr ON t.id = utr.task.id 
    WHERE utr.user.id = :userId 
    AND t.taskType = 'CLASS' 
    AND t.isDeleted = false 
    AND (
        (t.createdAt BETWEEN :startDate AND :endDate)
        OR 
        (t.deadline BETWEEN :startDate AND :endDate)
    )
    ORDER BY t.deadline ASC
""")
    List<Task> findLinkedClassTasksInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 检查任务是否存在且未删除
     */
    boolean existsByIdAndIsDeletedFalse(Long taskId);

    /**
     * 查找用户可访问的任务（用于权限验证）
     */
    @Query("""
    SELECT t FROM Task t 
    WHERE t.id = :taskId AND t.isDeleted = false 
    AND (
        (t.taskType = 'PERSONAL' AND t.creator.id = :userId)
        OR 
        (t.taskType = 'CLASS' AND EXISTS (
            SELECT 1 FROM UserTaskRelation utr 
            WHERE utr.task.id = t.id AND utr.user.id = :userId
        ))
    )
""")
    Task findAccessibleTaskByUserAndTaskId(@Param("userId") Long userId, @Param("taskId") Long taskId);
    /**
     * 获取指定班级在指定时间范围内创建的所有任务ID列表
     * @param classId 班级ID
     * @param startTime 开始时间（包含）
     * @return 任务ID列表
     */
    @Query("SELECT t.id FROM Task t WHERE t.classEntity.id = :classId AND t.createdAt >= :startTime")
    List<Long> findTaskIdsByClassIdAndCreatedAtAfter(@Param("classId") Long classId, @Param("startTime") LocalDateTime startTime);
    /**
     * 获取用户在指定时间范围内的个人任务
     */
    @Query("""
    SELECT t FROM Task t 
    WHERE t.creator.id = :userId 
    AND t.taskType = 'PERSONAL' 
    AND t.isDeleted = false 
    AND (
        (t.createdAt BETWEEN :startDate AND :endDate)
        OR 
        (t.deadline BETWEEN :startDate AND :endDate)
    )
    ORDER BY t.deadline ASC
""")
    List<Task> findPersonalTasksInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}