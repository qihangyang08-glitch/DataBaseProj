// 4. TaskStatusUpdateDto.java - 任务状态更新DTO
package com.example.taskplanning.dto;

import com.example.taskplanning.entity.UserTaskRelation;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class TaskStatusUpdateDto {

    @NotNull(message = "任务状态不能为空")
    private UserTaskRelation.TaskStatus status;

    private LocalDateTime personalDeadline;

    private String personalNotes;

    // Getters and Setters
    public UserTaskRelation.TaskStatus getStatus() { return status; }
    public void setStatus(UserTaskRelation.TaskStatus status) { this.status = status; }

    public LocalDateTime getPersonalDeadline() { return personalDeadline; }
    public void setPersonalDeadline(LocalDateTime personalDeadline) { this.personalDeadline = personalDeadline; }

    public String getPersonalNotes() { return personalNotes; }
    public void setPersonalNotes(String personalNotes) { this.personalNotes = personalNotes; }
}

