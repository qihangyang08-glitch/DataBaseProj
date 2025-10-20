package com.example.taskplanning.service;

import com.example.taskplanning.entity.Task;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.entity.UserClassRelation;
import com.example.taskplanning.repository.TaskRepository;
import com.example.taskplanning.repository.UserClassRelationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service("securityService") // 确保Bean的名字是 "securityService"
@Transactional(readOnly = true) // 权限检查都是只读操作，在类级别开启只读事务
public class SecurityService {

    private final UserService userService;
    private final UserClassRelationRepository userClassRelationRepository;
    private final TaskRepository taskRepository; // <-- 1. 新增 TaskRepository 依赖

    @Autowired
    public SecurityService(UserService userService,
                           UserClassRelationRepository userClassRelationRepository,
                           TaskRepository taskRepository) { // <-- 2. 在构造函数中注入
        this.userService = userService;
        this.userClassRelationRepository = userClassRelationRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * 【重构】检查当前用户是否是指定班级的成员
     */
    public boolean isClassMember(Long classId) {
        try {
            User currentUser = userService.getCurrentUserEntity();
            // 在Repository中添加这个新方法，比原来的exists...更精确
            return userClassRelationRepository.existsByUser_IdAndClassEntity_IdAndStatus(
                    currentUser.getId(),
                    classId,
                    UserClassRelation.JoinStatus.APPROVED
            );
        } catch (Exception e) {
            log.error("检查班级成员权限时出错: classId={}, userId={}", classId, getUserIdSafely(), e);
            return false;
        }
    }

    /**
     * 【重构】检查当前用户是否可以管理指定班级（是否为ADMIN或OWNER）
     */
    public boolean canManageClass(Long classId) {
        try {
            User currentUser = userService.getCurrentUserEntity();
            Optional<UserClassRelation> relationOpt = userClassRelationRepository
                    .findByUser_IdAndClassEntity_IdAndStatus(
                            currentUser.getId(),
                            classId,
                            UserClassRelation.JoinStatus.APPROVED
                    );

            if (relationOpt.isEmpty()) {
                return false; // 不是成员，自然不能管理
            }

            UserClassRelation.RoleInClass role = relationOpt.get().getRole();
            return role == UserClassRelation.RoleInClass.ADMIN || role == UserClassRelation.RoleInClass.OWNER;
        } catch (Exception e) {
            log.error("检查班级管理权限时出错: classId={}, userId={}", classId, getUserIdSafely(), e);
            return false;
        }
    }

    /**
     * 【重构】检查当前用户是否是指定班级的创建者（Owner）
     */
    public boolean isClassOwner(Long classId) {
        // 与 canManageClass 逻辑类似，但更严格
        try {
            User currentUser = userService.getCurrentUserEntity();
            Optional<UserClassRelation> relationOpt = userClassRelationRepository
                    .findByUser_IdAndClassEntity_IdAndStatus(
                            currentUser.getId(),
                            classId,
                            UserClassRelation.JoinStatus.APPROVED
                    );

            return relationOpt.isPresent() && relationOpt.get().getRole() == UserClassRelation.RoleInClass.OWNER;
        } catch (Exception e) {
            log.error("检查班级创建者权限时出错: classId={}, userId={}", classId, getUserIdSafely(), e);
            return false;
        }
    }

    /**
     * 【全新实现】检查当前用户是否可以访问（查看）指定任务
     * 这是个人状态更新、查看任务详情等操作的基础权限
     */
    public boolean canAccessTask(Long taskId) {
        try {
            User currentUser = userService.getCurrentUserEntity();
            Task task = taskRepository.findById(taskId).orElse(null);

            if (task == null) {
                return false; // 任务不存在，自然无权访问
            }

            if (task.getTaskType() == Task.TaskType.PERSONAL) {
                // 如果是个人任务，只有创建者本人能访问
                return task.getCreator().getId().equals(currentUser.getId());
            } else if (task.getTaskType() == Task.TaskType.CLASS) {
                // 如果是班级任务，需要是该班级的成员才能访问
                if (task.getClassEntity() == null) return false; // 数据完整性保护
                return isClassMember(task.getClassEntity().getId());
            }
            return false;
        } catch (Exception e) {
            log.error("检查任务访问权限时出错: taskId={}, userId={}", taskId, getUserIdSafely(), e);
            return false;
        }
    }

    /**
     * 【全新实现】检查当前用户是否可以编辑或删除指定任务
     */
    public boolean canEditTask(Long taskId) {
        try {
            User currentUser = userService.getCurrentUserEntity();
            Task task = taskRepository.findById(taskId).orElse(null);

            if (task == null) {
                return false;
            }

            if (task.getTaskType() == Task.TaskType.PERSONAL) {
                // 个人任务，只有创建者能编辑
                return task.getCreator().getId().equals(currentUser.getId());
            } else if (task.getTaskType() == Task.TaskType.CLASS) {
                // 班级任务，只有班级管理员能编辑
                if (task.getClassEntity() == null) return false;
                return canManageClass(task.getClassEntity().getId());
            }
            return false;
        } catch (Exception e) {
            log.error("检查任务编辑权限时出错: taskId={}, userId={}", taskId, getUserIdSafely(), e);
            return false;
        }
    }

    /**
     * 一个安全的辅助方法，用于在日志中获取用户ID，避免空指针
     */
    private Long getUserIdSafely() {
        try {
            return userService.getCurrentUserEntity().getId();
        } catch (Exception e) {
            return null; // 如果获取用户失败，返回null
        }
    }
}