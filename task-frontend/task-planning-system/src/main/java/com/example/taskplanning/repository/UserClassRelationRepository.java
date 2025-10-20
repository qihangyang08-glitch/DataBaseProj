// 2. UserClassRelationRepository.java
package com.example.taskplanning.repository;

import com.example.taskplanning.entity.Classes;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.entity.UserClassRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserClassRelationRepository extends JpaRepository<UserClassRelation, Long> {

    /**
     * 查找用户和班级之间的关系记录
     * @param user 用户实体
     * @param classEntity 班级实体
     * @return 用户班级关系的Optional包装
     */
    Optional<UserClassRelation> findByUserAndClassEntity(User user, Classes classEntity);

    /**
     * 分页查询指定班级中指定状态的用户关系记录
     * @param classEntity 班级实体
     * @param status 关系状态
     * @param pageable 分页参数
     * @return 分页的用户班级关系记录
     */
    Page<UserClassRelation> findByClassEntityAndStatus(
            Classes classEntity,
            UserClassRelation.JoinStatus status,
            Pageable pageable
    );

    /**
     * 分页查询指定用户的指定状态的班级关系记录
     * @param user 用户实体
     * @param status 关系状态
     * @param pageable 分页参数
     * @return 分页的用户班级关系记录
     */
    Page<UserClassRelation> findByUserAndStatus(
            User user,
            UserClassRelation.JoinStatus status,
            Pageable pageable
    );

    /**
     * 查询用户在指定班级中的角色和状态
     * 这个方法用于权限检查，确保用户是ADMIN或OWNER
     * @param userId 用户ID
     * @param classId 班级ID
     * @param role 用户角色
     * @param status 关系状态
     * @return 用户班级关系的Optional包装
     */
    @Query("SELECT ucr FROM UserClassRelation ucr " +
            "WHERE ucr.user.id = :userId " +
            "AND ucr.classEntity.id = :classId " +
            "AND ucr.role = :role " +
            "AND ucr.status = :status")
    Optional<UserClassRelation> findByUserIdAndClassIdAndRoleAndStatus(
            @Param("userId") Long userId,
            @Param("classId") Long classId,
            @Param("role") UserClassRelation.RoleInClass role,
            @Param("status") UserClassRelation.JoinStatus status
    );

    /**
     * 检查用户是否在指定班级中拥有管理权限（ADMIN或OWNER）
     * @param userId 用户ID
     * @param classId 班级ID
     * @return 如果用户是ADMIN或OWNER且状态为APPROVED则返回true
     */
    @Query("SELECT CASE WHEN COUNT(ucr) > 0 THEN true ELSE false END " +
            "FROM UserClassRelation ucr " +
            "WHERE ucr.user.id = :userId " +
            "AND ucr.classEntity.id = :classId " +
            "AND ucr.role IN ('ADMIN', 'OWNER') " +
            "AND ucr.status = 'APPROVED'")
    boolean hasAdminPermission(
            @Param("userId") Long userId,
            @Param("classId") Long classId
    );

    /**
     * 查找指定用户和班级之间的PENDING状态申请记录
     * @param userId 申请用户ID
     * @param classId 班级ID
     * @return PENDING状态的关系记录
     */
    @Query("SELECT ucr FROM UserClassRelation ucr " +
            "WHERE ucr.user.id = :userId " +
            "AND ucr.classEntity.id = :classId " +
            "AND ucr.status = 'PENDING'")
    Optional<UserClassRelation> findPendingApplicationByUserIdAndClassId(
            @Param("userId") Long userId,
            @Param("classId") Long classId
    );

    /**
     * 检查用户是否为班级的管理员或创建者
     */
    boolean existsByUserIdAndClassEntityIdAndRoleInAndStatus(
            Long userId,
            Long classId,
            List<UserClassRelation.RoleInClass> roles,
            UserClassRelation.JoinStatus status
    );

    /**
     * 检查用户是否为班级成员
     */
    boolean existsByUserIdAndClassEntityIdAndStatus(
            Long userId,
            Long classId,
            UserClassRelation.JoinStatus status
    );

    /**
     * 查找用户在所有已批准班级中的关系
     */
    List<UserClassRelation> findByUserIdAndStatus(Long userId, UserClassRelation.JoinStatus status);

    // ===== 新增方法：支持角色管理功能 =====

    /**
     * 检查用户是否是指定班级的指定角色且状态为已批准
     * 用于检查是否为班级创建者(Owner)
     * @param userId 用户ID
     * @param classId 班级ID
     * @param role 角色
     * @param status 状态
     * @return 是否存在匹配的记录
     */
    boolean existsByUserIdAndClassEntityIdAndRoleAndStatus(
            Long userId,
            Long classId,
            UserClassRelation.RoleInClass role,
            UserClassRelation.JoinStatus status
    );

    /**
     * 根据用户ID、班级ID和状态查找关系记录
     * 用于角色管理时查找要修改的成员记录
     * @param userId 用户ID
     * @param classId 班级ID
     * @param status 状态
     * @return 关系记录的Optional包装
     */
    @Query("SELECT ucr FROM UserClassRelation ucr " +
            "WHERE ucr.user.id = :userId " +
            "AND ucr.classEntity.id = :classId " +
            "AND ucr.status = :status")
    Optional<UserClassRelation> findByUserIdAndClassEntityIdAndStatus(
            @Param("userId") Long userId,
            @Param("classId") Long classId,
            @Param("status") UserClassRelation.JoinStatus status
    );
    // 用于 isClassMember
    boolean existsByUser_IdAndClassEntity_IdAndStatus(Long userId, Long classId, UserClassRelation.JoinStatus status);

    // 用于 canManageClass 和 isClassOwner
    Optional<UserClassRelation> findByUser_IdAndClassEntity_IdAndStatus(Long userId, Long classId, UserClassRelation.JoinStatus status);


    /**
     * 根据班级ID和成员状态，分页查询关系记录
     * Spring Data JPA会自动根据方法名生成查询
     * @param classId 班级ID
     * @param status 成员的加入状态
     * @param pageable 分页参数
     * @return 分页的关系记录
     */
    Page<UserClassRelation> findByClassEntity_IdAndStatus(Long classId, UserClassRelation.JoinStatus status, Pageable pageable);
    /**
     * 查找指定管理员名下所有班级的待审批申请（分页）
     * @param managerId 管理员（OWNER或ADMIN）的用户ID
     * @param status 申请状态，应为 PENDING
     * @param pageable 分页参数
     * @return 分页的待审批关系记录
     */
    @Query("SELECT ucr FROM UserClassRelation ucr WHERE ucr.status = :status AND EXISTS (" +
            "SELECT 1 FROM UserClassRelation managerRel " +
            "WHERE managerRel.classEntity = ucr.classEntity " +
            "AND managerRel.user.id = :managerId " +
            "AND managerRel.role IN ('OWNER', 'ADMIN') " +
            "AND managerRel.status = 'APPROVED'" +
            ") ORDER BY ucr.createdAt DESC")
    Page<UserClassRelation> findPendingApprovalsForManager(
            @Param("managerId") Long managerId,
            @Param("status") UserClassRelation.JoinStatus status,
            Pageable pageable
    );
}