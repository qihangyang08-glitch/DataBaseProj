// 9. TaskService.java - 任务业务逻辑服务
package com.example.taskplanning.service;

import com.example.taskplanning.dto.CalendarTaskDto;
import com.example.taskplanning.dto.TaskCreateDto;
import com.example.taskplanning.dto.TaskResponseDto;
import com.example.taskplanning.dto.TaskStatusUpdateDto;
import com.example.taskplanning.entity.*;
import com.example.taskplanning.exception.BusinessException;
import com.example.taskplanning.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.example.taskplanning.annotation.LogAction;

@Service
@Transactional
public class TaskService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserTaskRelationRepository userTaskRelationRepository;
    private final UserClassRelationRepository userClassRelationRepository;
    private final ClassRepository classRepository;
    private final UserService userService;

    @Autowired
    public TaskService(TaskRepository taskRepository,
                       UserTaskRelationRepository userTaskRelationRepository,
                       UserClassRelationRepository userClassRelationRepository,
                       ClassRepository classRepository,
                       UserService userService) {
        this.taskRepository = taskRepository;
        this.userTaskRelationRepository = userTaskRelationRepository;
        this.userClassRelationRepository = userClassRelationRepository;
        this.classRepository = classRepository;
        this.userService = userService;
    }

    /**
     * 创建个人任务
     */
    @LogAction(action = "TASK_CREATE_PERSONAL", entityType = "TASK")
    public TaskResponseDto createPersonalTask(TaskCreateDto createDto) {
        User currentUser = userService.getCurrentUserEntity();

        Task task = new Task();
        task.setTitle(createDto.getTitle());
        task.setDescription(createDto.getDescription());
        task.setCourseName(createDto.getCourseName());
        task.setTaskType(Task.TaskType.PERSONAL);
        task.setDeadline(createDto.getDeadline());
        task.setCreator(currentUser);
        task.setClassEntity(null); // 个人任务无关联班级

        Task savedTask = taskRepository.save(task);
        return convertToResponseDto(savedTask, null);
    }

    /**
     * 创建班级任务
     */
    @LogAction(action = "TASK_CREATE_CLASS", entityType = "TASK")
    public TaskResponseDto createClassTask(Long classId, TaskCreateDto createDto) {
        User currentUser = userService.getCurrentUserEntity();

        // 验证班级存在
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("CLASS_NOT_FOUND", "班级不存在"));

        Task task = new Task();
        task.setTitle(createDto.getTitle());
        task.setDescription(createDto.getDescription());
        task.setCourseName(createDto.getCourseName());
        task.setTaskType(Task.TaskType.CLASS);
        task.setDeadline(createDto.getDeadline());
        task.setCreator(currentUser);
        task.setClassEntity(classEntity);

        Task savedTask = taskRepository.save(task);
        return convertToResponseDto(savedTask, null);
    }

    /**
     * 获取日历视图数据
     * 严格遵循"用户自主控制"原则：只显示用户主动关联的任务
     */
    @Transactional(readOnly = true)
    public List<CalendarTaskDto> getCalendarTasks(int year, int month) {
        User currentUser = userService.getCurrentUserEntity();

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 使用修正后的查询方法，确保只返回用户主动关联的任务
        List<Task> tasks = taskRepository.findUserRelatedTasksInDateRange(
                currentUser.getId(), startDate, endDate);

        // 获取用户的所有任务关系，用于填充个人状态
        Map<Long, UserTaskRelation> userRelations = userTaskRelationRepository
                .findByUserId(currentUser.getId())
                .stream()
                .collect(Collectors.toMap(
                        relation -> relation.getTask().getId(),
                        relation -> relation
                ));

        return tasks.stream()
                .map(task -> convertToCalendarDto(task, userRelations.get(task.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 获取班级任务列表
     */
    @Transactional(readOnly = true)
    public Page<TaskResponseDto> getClassTasks(Long classId, Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();

        Page<Task> tasks = taskRepository.findByClassEntityIdAndIsDeletedFalseOrderByDeadlineAsc(classId, pageable);

        // 获取用户的任务关系映射
        Map<Long, UserTaskRelation> userRelations = userTaskRelationRepository
                .findByUserId(currentUser.getId())
                .stream()
                .collect(Collectors.toMap(
                        relation -> relation.getTask().getId(),
                        relation -> relation
                ));

        return tasks.map(task -> convertToResponseDto(task, userRelations.get(task.getId())));
    }

    /**
     * 更新任务的个人状态
     * 🔥 修复：明确区分个人任务和班级任务的处理逻辑
     */
    public TaskResponseDto updateTaskStatus(Long taskId, TaskStatusUpdateDto statusUpdateDto) {
        User currentUser = userService.getCurrentUserEntity();

        // 验证用户是否可以访问该任务
        Task task = taskRepository.findAccessibleTaskByUserAndTaskId(currentUser.getId(), taskId);
        if (task == null) {
            throw new BusinessException("TASK_NOT_ACCESSIBLE", "无权访问该任务");
        }

        // 统一通过关系表管理所有任务的个人状态
        // 个人任务和班级任务都使用相同的逻辑，因为本质上都是"用户对任务的个人化设定"
        return updateUserTaskRelation(currentUser, task, statusUpdateDto);
    }
    /**
     * 获取任务详情
     */
    @Transactional(readOnly = true)
    public TaskResponseDto getTaskDetail(Long taskId) {
        User currentUser = userService.getCurrentUserEntity();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "任务不存在"));
        // 如果任务已删除，直接返回不存在
        if (task.isDeleted()) {
            throw new BusinessException("TASK_NOT_FOUND", "任务不存在");
        }

        // 检查用户是否可以访问该任务
        if (!canUserAccessTask(currentUser.getId(), taskId)) {
            throw new BusinessException("TASK_NOT_ACCESSIBLE", "无权访问该任务");
        }


        UserTaskRelation relation = userTaskRelationRepository
                .findByUserIdAndTaskId(currentUser.getId(), taskId)
                .orElse(null);

        return convertToResponseDto(task, relation);
    }

    /**
     * 更新或创建用户任务关系
     * 🔥 修复：确保新建关系时正确设置personal_deadline默认值
     */
    private TaskResponseDto updateUserTaskRelation(User user, Task task, TaskStatusUpdateDto statusUpdateDto) {
        UserTaskRelation relation = userTaskRelationRepository
                .findByUserIdAndTaskId(user.getId(), task.getId())
                .orElseGet(() -> {
                    UserTaskRelation newRelation = new UserTaskRelation();
                    newRelation.setUser(user);
                    newRelation.setTask(task);

                    // 🔥 关键修复：为新建的关系设置personal_deadline默认值
                    // 这处理了"手动导入"任务到个人日历的场景
                    if (newRelation.getPersonalDeadline() == null) {
                        newRelation.setPersonalDeadline(task.getDeadline());
                    }

                    return newRelation;
                });
        // Log before update
        try { logger.info("updateUserTaskRelation before: taskId={} relationId={} personalDeadline={} status={}", task.getId(), relation.getId(), relation.getPersonalDeadline(), relation.getStatus()); } catch (Exception e) {}

        // 更新用户提供的字段
        relation.setStatus(statusUpdateDto.getStatus());

        // 只有当用户明确提供了personal_deadline时才更新，否则保持现有值
        if (statusUpdateDto.getPersonalDeadline() != null) {
            relation.setPersonalDeadline(statusUpdateDto.getPersonalDeadline());
        }

        relation.setPersonalNotes(statusUpdateDto.getPersonalNotes());

        // 如果状态是DONE，记录完成时间
        if (statusUpdateDto.getStatus() == UserTaskRelation.TaskStatus.DONE) {
            relation.setCompletedAt(LocalDateTime.now());
        } else {
            relation.setCompletedAt(null);
        }

        UserTaskRelation savedRelation = userTaskRelationRepository.save(relation);

        // Log after save
        try { logger.info("updateUserTaskRelation after: taskId={} relationId={} personalDeadline={} status={}", task.getId(), savedRelation.getId(), savedRelation.getPersonalDeadline(), savedRelation.getStatus()); } catch (Exception e) {}

        return convertToResponseDto(task, savedRelation);
    }

    /**
     * 检查用户是否可以访问任务
     */
    @Transactional(readOnly = true)
    public boolean canUserAccessTask(Long userId, Long taskId) {
        Task task = taskRepository.findAccessibleTaskByUserAndTaskId(userId, taskId);
        return task != null;
    }

    /**
     * 转换为响应DTO
     */
    private TaskResponseDto convertToResponseDto(Task task, UserTaskRelation relation) {
        TaskResponseDto dto = new TaskResponseDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setCourseName(task.getCourseName());
        dto.setTaskType(task.getTaskType());
        dto.setDeadline(task.getDeadline());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        dto.setCreatorId(task.getCreator().getId());
        dto.setCreatorName(task.getCreator().getName());

        if (task.getClassEntity() != null) {
            dto.setClassId(task.getClassEntity().getId());
            dto.setClassName(task.getClassEntity().getName());
        }

        if (relation != null) {
            dto.setPersonalStatus(relation.getStatus());
            dto.setPersonalDeadline(relation.getPersonalDeadline());
            dto.setPersonalNotes(relation.getPersonalNotes());
            dto.setCompletedAt(relation.getCompletedAt());
        } else {
            dto.setPersonalStatus(UserTaskRelation.TaskStatus.TODO);
        }

        return dto;
    }

    /**
     * 转换为日历DTO
     */
    private CalendarTaskDto convertToCalendarDto(Task task, UserTaskRelation relation) {
        CalendarTaskDto dto = new CalendarTaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription()); // <-- 新增的逻辑
        dto.setCourseName(task.getCourseName());
        dto.setTaskType(task.getTaskType());
        dto.setDeadline(task.getDeadline());
        dto.setCreatedAt(task.getCreatedAt());

        if (task.getClassEntity() != null) {
            dto.setClassName(task.getClassEntity().getName());
        }

        if (relation != null) {
            dto.setPersonalStatus(relation.getStatus());
            dto.setPersonalDeadline(relation.getPersonalDeadline());
        } else {
            dto.setPersonalStatus(UserTaskRelation.TaskStatus.TODO);
        }

        return dto;
    }
    /**
     * 获取用户的个人任务列表
     */
    @Transactional(readOnly = true)
    public Page<TaskResponseDto> getPersonalTasks(Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();

        Page<Task> tasks = taskRepository.findByCreatorIdAndTaskTypeAndIsDeletedFalseOrderByDeadlineAsc(
                currentUser.getId(), Task.TaskType.PERSONAL, pageable);

        // 获取用户的任务关系映射
        Map<Long, UserTaskRelation> userRelations = userTaskRelationRepository
                .findByUserId(currentUser.getId())
                .stream()
                .collect(Collectors.toMap(
                        relation -> relation.getTask().getId(),
                        relation -> relation
                ));

        return tasks.map(task -> convertToResponseDto(task, userRelations.get(task.getId())));
    }

    /**
     * 删除任务（软删除）
     * 🔥 修复：完善班级任务的删除权限校验逻辑
     */
    public void deleteTask(Long taskId) {
        User currentUser = userService.getCurrentUserEntity();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "任务不存在"));

        // 🔥 关键修复：根据业务规则二，完善删除权限校验
        boolean hasPermission = false;

        if (task.getTaskType() == Task.TaskType.PERSONAL) {
            // 个人任务：仅创建者可删除
            hasPermission = task.getCreator().getId().equals(currentUser.getId());
        } else if (task.getTaskType() == Task.TaskType.CLASS) {
            // 班级任务：创建者或该班级的ADMIN/OWNER可删除
            if (task.getCreator().getId().equals(currentUser.getId())) {
                hasPermission = true;
            } else {
                // 检查是否为班级管理员
                Long classId = task.getClassEntity().getId();
                hasPermission = userClassRelationRepository.hasAdminPermission(currentUser.getId(), classId);
            }
        }

        if (!hasPermission) {
            throw new BusinessException("TASK_DELETE_FORBIDDEN", "无权删除该任务");
        }

        task.setDeleted(true);
        taskRepository.save(task);
    }

    /**
     * 更新任务信息（仅创建者或有权限的管理员可操作）
     * 🔥 修复：完善班级任务的权限校验逻辑
     */
    public TaskResponseDto updateTask(Long taskId, TaskCreateDto updateDto) {
        User currentUser = userService.getCurrentUserEntity();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "任务不存在"));

        // 🔥 关键修复：根据业务规则二，完善权限校验
        boolean hasPermission = false;

        if (task.getTaskType() == Task.TaskType.PERSONAL) {
            // 个人任务：仅创建者可修改
            hasPermission = task.getCreator().getId().equals(currentUser.getId());
        } else if (task.getTaskType() == Task.TaskType.CLASS) {
            // 班级任务：创建者或该班级的ADMIN/OWNER可修改
            if (task.getCreator().getId().equals(currentUser.getId())) {
                hasPermission = true;
            } else {
                // 检查是否为班级管理员
                Long classId = task.getClassEntity().getId();
                hasPermission = userClassRelationRepository.hasAdminPermission(currentUser.getId(), classId);
            }
        }

        if (!hasPermission) {
            throw new BusinessException("TASK_UPDATE_FORBIDDEN", "无权修改该任务");
        }

        // 执行更新
        task.setTitle(updateDto.getTitle());
        task.setDescription(updateDto.getDescription());
        task.setCourseName(updateDto.getCourseName());
        task.setDeadline(updateDto.getDeadline());

        Task savedTask = taskRepository.save(task);

        UserTaskRelation relation = userTaskRelationRepository
                .findByUserIdAndTaskId(currentUser.getId(), taskId)
                .orElse(null);

        return convertToResponseDto(savedTask, relation);
    }

}
