package com.example.taskplanning.service;

import com.example.taskplanning.dto.SyncResultDto;
import com.example.taskplanning.entity.Task;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.entity.UserTaskRelation;
import com.example.taskplanning.repository.TaskRepository;
import com.example.taskplanning.repository.UserTaskRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.taskplanning.annotation.LogAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SyncService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserTaskRelationRepository userTaskRelationRepository;

    @Autowired
    private UserService userService;

    /**
     * 为用户智能同步指定班级在指定时间范围内的任务
     * @param classId 班级ID
     * @param range 时间范围
     * @return 同步结果
     */
    @Transactional
    @LogAction(action = "TASK_SYNC", entityType = "CLASS")
    public SyncResultDto syncClassTasks(Long classId, String range) {
        // 1. 获取当前登录用户
        User currentUser = userService.getCurrentUserEntity();

        // 2. 计算时间范围
        LocalDateTime startTime = calculateStartTime(range);

        // 3. 第一轮筛选：获取班级在指定时间范围内的所有任务ID
        List<Long> candidateTaskIds = taskRepository.findTaskIdsByClassIdAndCreatedAtAfter(classId, startTime);

        // 4. 获取用户已关联的任务ID列表
        List<Long> existingTaskIds = userTaskRelationRepository.findTaskIdsByUserId(currentUser.getId());

        // 5. 计算差集：找出需要新增关联的任务ID
        Set<Long> existingTaskIdSet = new HashSet<>(existingTaskIds);
        List<Long> newTaskIdsToSync = new ArrayList<>();

        for (Long taskId : candidateTaskIds) {
            if (!existingTaskIdSet.contains(taskId)) {
                newTaskIdsToSync.add(taskId);
            }
        }

        // 6. 批量创建关联
        if (!newTaskIdsToSync.isEmpty()) {
            List<UserTaskRelation> newRelations = new ArrayList<>();

            for (Long taskId : newTaskIdsToSync) {
                // 创建任务引用对象，JPA会处理延迟加载
                Task taskReference = taskRepository.getReferenceById(taskId);

                UserTaskRelation relation = new UserTaskRelation();
                relation.setUser(currentUser);
                relation.setTask(taskReference);
                relation.setStatus(UserTaskRelation.TaskStatus.TODO); // 默认状态

                // 🔥 关键修复：根据业务规则三，设置personal_deadline的默认值
                // 将任务的官方deadline作为用户个人计划的初始值
                relation.setPersonalDeadline(taskReference.getDeadline());

                relation.setCreatedAt(LocalDateTime.now());
                relation.setUpdatedAt(LocalDateTime.now());

                newRelations.add(relation);
            }

            // 批量保存
            userTaskRelationRepository.saveAll(newRelations);
        }

        // 7. 构建并返回结果
        return new SyncResultDto(
                newTaskIdsToSync.size(),
                range,
                candidateTaskIds.size()
        );
    }

    /**
     * 根据范围字符串计算开始时间
     * @param range 时间范围
     * @return 开始时间
     */
    private LocalDateTime calculateStartTime(String range) {
        LocalDateTime now = LocalDateTime.now();

        switch (range.toLowerCase()) {
            case "day":
                return now.minusDays(1);
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            case "semester":
                return now.minusMonths(6);
            case "year":
                return now.minusYears(1);
            default:
                throw new IllegalArgumentException("不支持的时间范围: " + range);
        }
    }
}