// 1. ClassRepository.java
package com.example.taskplanning.repository;

import com.example.taskplanning.entity.Classes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<Classes, Long> {

    /**
     * 根据邀请码查找班级
     * @param inviteCode 邀请码
     * @return 班级实体的Optional包装
     */
    Optional<Classes> findByInviteCode(String inviteCode);
}


