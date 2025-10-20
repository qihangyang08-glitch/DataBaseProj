// 1. ClassRepository.java
package com.example.taskplanning.repository;

import com.example.taskplanning.entity.Classes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<Classes, Long> {

    /**
     * 根据邀请码查找班级
     * @param inviteCode 邀请码
     * @return 班级实体的Optional包装
     */
    Optional<Classes> findByInviteCode(String inviteCode);
    /**
     * 根据班级名称模糊查询公开的班级（分页）
     * @param name 查询的名称片段
     * @param pageable 分页参数
     * @return 分页的班级实体列表
     */
    Page<Classes> findByNameContainingAndIsPublicTrue(String name, Pageable pageable);

}


