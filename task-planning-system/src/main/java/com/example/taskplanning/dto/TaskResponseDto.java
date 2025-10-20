// 3. TaskResponseDto.java - 任务响应DTO
package com.example.taskplanning.dto;

import com.example.taskplanning.entity.Task;
import com.example.taskplanning.entity.UserTaskRelation;
import java.time.LocalDateTime;

public class TaskResponseDto {

    private Long id;
    private String title;
    private String description;
    private String courseName;
    private Task.TaskType taskType;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 创建者信息
    private Long creatorId;
    private String creatorName;

    // 班级信息（如果是班级任务）
    private Long classId;
    private String className;

    // 个人状态信息（如果用户有个人关系记录）
    private UserTaskRelation.TaskStatus personalStatus;
    private LocalDateTime personalDeadline;
    private String personalNotes;
    private LocalDateTime completedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Task.TaskType getTaskType() { return taskType; }
    public void setTaskType(Task.TaskType taskType) { this.taskType = taskType; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public UserTaskRelation.TaskStatus getPersonalStatus() { return personalStatus; }
    public void setPersonalStatus(UserTaskRelation.TaskStatus personalStatus) { this.personalStatus = personalStatus; }

    public LocalDateTime getPersonalDeadline() { return personalDeadline; }
    public void setPersonalDeadline(LocalDateTime personalDeadline) { this.personalDeadline = personalDeadline; }

    public String getPersonalNotes() { return personalNotes; }
    public void setPersonalNotes(String personalNotes) { this.personalNotes = personalNotes; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
