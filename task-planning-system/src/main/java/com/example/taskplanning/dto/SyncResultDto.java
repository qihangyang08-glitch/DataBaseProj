package com.example.taskplanning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "同步结果响应")
public class SyncResultDto {

    @Schema(description = "本次同步新增的任务数量")
    private int newlySyncedTasks;

    @Schema(description = "本次同步使用的范围")
    private String syncRange;

    @Schema(description = "在该时间范围内，班级总共的任务数量")
    private int totalTasksInClassInRange;

    public SyncResultDto() {}

    public SyncResultDto(int newlySyncedTasks, String syncRange, int totalTasksInClassInRange) {
        this.newlySyncedTasks = newlySyncedTasks;
        this.syncRange = syncRange;
        this.totalTasksInClassInRange = totalTasksInClassInRange;
    }

    // Getters and Setters
    public int getNewlySyncedTasks() {
        return newlySyncedTasks;
    }

    public void setNewlySyncedTasks(int newlySyncedTasks) {
        this.newlySyncedTasks = newlySyncedTasks;
    }

    public String getSyncRange() {
        return syncRange;
    }

    public void setSyncRange(String syncRange) {
        this.syncRange = syncRange;
    }

    public int getTotalTasksInClassInRange() {
        return totalTasksInClassInRange;
    }

    public void setTotalTasksInClassInRange(int totalTasksInClassInRange) {
        this.totalTasksInClassInRange = totalTasksInClassInRange;
    }
}
