// 10. TaskController.java - 任务控制器
package com.example.taskplanning.controller;

import com.example.taskplanning.ApiResponse;
import com.example.taskplanning.dto.CalendarTaskDto;
import com.example.taskplanning.dto.TaskCreateDto;
import com.example.taskplanning.dto.TaskResponseDto;
import com.example.taskplanning.dto.TaskStatusUpdateDto;
import com.example.taskplanning.entity.Task;
import com.example.taskplanning.entity.UserTaskRelation;
import com.example.taskplanning.exception.BusinessException;
import com.example.taskplanning.service.TaskService;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 创建个人任务
     */
    @PostMapping("/tasks/personal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponseDto>> createPersonalTask(
            @Valid @RequestBody TaskCreateDto createDto) {

        TaskResponseDto task = taskService.createPersonalTask(createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(task, "个人任务创建成功"));
    }

    /**
     * 创建班级任务
     */
    @PostMapping("/classes/{classId}/tasks")
    @PreAuthorize("@securityService.canManageClass(#classId)")
    public ResponseEntity<ApiResponse<TaskResponseDto>> createClassTask(
            @PathVariable Long classId,
            @Valid @RequestBody TaskCreateDto createDto) {

        TaskResponseDto task = taskService.createClassTask(classId, createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(task, "班级任务创建成功"));
    }

    /**
     * 获取日历视图数据
     */
    @GetMapping("/calendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CalendarTaskDto>>> getCalendarTasks(
            @RequestParam int year,
            @RequestParam int month) {

        List<CalendarTaskDto> tasks = taskService.getCalendarTasks(year, month);
        return ResponseEntity.ok(ApiResponse.success(tasks, "日历数据获取成功"));
    }

    /**
     * 获取班级任务列表
     */
    @GetMapping("/classes/{classId}/tasks")
    @PreAuthorize("@securityService.isClassMember(#classId)")
    public ResponseEntity<ApiResponse<Page<TaskResponseDto>>> getClassTasks(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResponseDto> tasks = taskService.getClassTasks(classId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks, "班级任务列表获取成功"));
    }

    /**
     * 更新任务的个人状态
     */
    @PutMapping("/tasks/{taskId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponseDto>> updateTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateDto statusUpdateDto) {

        // Log incoming DTO for debugging personalDeadline issues
        try {
            logger.info("updateTaskStatus called for taskId={} status={} personalDeadline={}", taskId, statusUpdateDto.getStatus(), statusUpdateDto.getPersonalDeadline());
        } catch (Exception e) {
            logger.warn("Failed to log updateTaskStatus payload", e);
        }

        TaskResponseDto task = taskService.updateTaskStatus(taskId, statusUpdateDto);
        logger.info("updateTaskStatus completed for taskId={}, returned personalDeadline={}", taskId, task.getPersonalDeadline());
        return ResponseEntity.ok(ApiResponse.success(task, "任务状态更新成功"));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponseDto>> getTaskDetail(@PathVariable Long taskId) {
        TaskResponseDto task = taskService.getTaskDetail(taskId);
        return ResponseEntity.ok(ApiResponse.success(task, "任务详情获取成功"));
    }
    /**
     * 获取个人任务列表
     */
    @GetMapping("/tasks/personal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<TaskResponseDto>>> getPersonalTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResponseDto> tasks = taskService.getPersonalTasks(pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks, "个人任务列表获取成功"));
    }

    /**
     * 更新任务信息
     */
    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponseDto>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskCreateDto updateDto) {

        TaskResponseDto task = taskService.updateTask(taskId, updateDto);
        return ResponseEntity.ok(ApiResponse.success(task, "任务更新成功"));
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(ApiResponse.success("任务删除成功"));
    }
}
