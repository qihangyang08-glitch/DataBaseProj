// 7. UserTaskRelationRepository.java - 用户任务关系数据访问接口
package com.example.taskplanning.repository;

import com.example.taskplanning.entity.UserClassRelation;
import com.example.taskplanning.entity.UserTaskRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserTaskRelationRepository extends JpaRepository<UserTaskRelation, Long> {

    /**
     * 查找用户与特定任务的关系
     */
    Optional<UserTaskRelation> findByUserIdAndTaskId(Long userId, Long taskId);

    /**
     * 查找用户的所有任务关系
     */
    List<UserTaskRelation> findByUserId(Long userId);

    /**
     * 检查用户与任务的关系是否存在
     */
    boolean existsByUserIdAndTaskId(Long userId, Long taskId);

    /**
     * 删除用户与任务的关系
     */
    void deleteByUserIdAndTaskId(Long userId, Long taskId);

    /**
     * 获取指定用户已关联的所有任务ID列表
     * @param userId 用户ID
     * @return 任务ID列表
     */
    @Query("SELECT utr.task.id FROM UserTaskRelation utr WHERE utr.user.id = :userId")
    List<Long> findTaskIdsByUserId(@Param("userId") Long userId);
}

