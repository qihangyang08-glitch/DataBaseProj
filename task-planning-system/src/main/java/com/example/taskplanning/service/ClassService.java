// ClassService.java
package com.example.taskplanning.service;

import com.example.taskplanning.annotation.LogAction;
import com.example.taskplanning.dto.*;
import com.example.taskplanning.entity.Classes;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.entity.UserClassRelation;
import com.example.taskplanning.exception.BusinessException;
import com.example.taskplanning.repository.ClassRepository;
import com.example.taskplanning.repository.UserClassRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class ClassService {

    private final ClassRepository classRepository;
    private final UserClassRelationRepository userClassRelationRepository;
    private final UserService userService;

    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    public ClassService(ClassRepository classRepository,
                        UserClassRelationRepository userClassRelationRepository,
                        UserService userService) {
        this.classRepository = classRepository;
        this.userClassRelationRepository = userClassRelationRepository;
        this.userService = userService;
    }

    /**
     * 创建班级
     * @param createDto 创建班级的DTO
     * @return 创建的班级响应DTO
     */
    @LogAction(action = "CLASS_CREATE", entityType = "CLASS")
    public ClassResponseDto createClass(ClassCreateDto createDto) {
        // 获取当前登录用户
        User creator = userService.getCurrentUserEntity();

        // 生成唯一的邀请码
        String inviteCode = generateUniqueInviteCode();

        // 创建班级实体
        Classes classEntity = new Classes();
        classEntity.setName(createDto.getName());
        classEntity.setDescription(createDto.getDescription());
        classEntity.setInviteCode(inviteCode);
        classEntity.setPublic(createDto.isPublic());
        classEntity.setJoinApprovalRequired(createDto.isJoinApprovalRequired());
        classEntity.setCreatedAt(LocalDateTime.now());
        classEntity.setOwner(creator);

        // 保存班级
        Classes savedClass = classRepository.save(classEntity);

        // 在同一事务中创建创建者的班级关系记录
        UserClassRelation ownerRelation = new UserClassRelation();
        ownerRelation.setUser(creator);
        ownerRelation.setClassEntity(savedClass);
        ownerRelation.setRole(UserClassRelation.RoleInClass.OWNER);
        ownerRelation.setStatus(UserClassRelation.JoinStatus.APPROVED);
        ownerRelation.setJoinedAt(LocalDateTime.now());
        ownerRelation.setCreatedAt(LocalDateTime.now());

        userClassRelationRepository.save(ownerRelation);

        return convertToClassResponseDto(savedClass);
    }

    /**
     * 根据邀请码查找班级
     * @param inviteCode 邀请码
     * @return 班级响应DTO
     * @throws BusinessException 如果班级不存在
     */
    @Transactional(readOnly = true)
    public ClassResponseDto findClassByInviteCode(String inviteCode) {
        Classes classEntity = classRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException("PermissionDeniedException", "邀请码无效或班级不存在"));

        return convertToClassResponseDto(classEntity);
    }

    /**
     * 申请加入班级
     * @param classId 班级ID
     * @param joinRequestDto 加入申请DTO
     * @throws BusinessException 如果班级不存在或用户已是成员/已有待处理申请
     */
    @LogAction(action = "CLASS_JOIN_APPLY", entityType = "CLASS")
    public void applyToJoinClass(Long classId, ClassJoinRequestDto joinRequestDto) {
        // 获取当前登录用户
        User applicant = userService.getCurrentUserEntity();

        // 查找班级
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("applyToJoinClass_FAILED", "班级不存在"));

        // 检查用户是否已经是成员或已有待处理申请
        Optional<UserClassRelation> existingRelation =
                userClassRelationRepository.findByUserAndClassEntity(applicant, classEntity);

        if (existingRelation.isPresent()) {
            UserClassRelation relation = existingRelation.get();
            if (relation.getStatus() == UserClassRelation.JoinStatus.APPROVED) {
                throw new BusinessException("applyToJoinClass_FAILED", "您已经是该班级的成员");
            } else if (relation.getStatus() == UserClassRelation.JoinStatus.PENDING) {
                throw new BusinessException("applyToJoinClass_FAILED", "您已有待处理的加入申请");
            }
        }

        // 创建新的申请记录
        UserClassRelation newRelation = new UserClassRelation();
        newRelation.setUser(applicant);
        newRelation.setClassEntity(classEntity);
        newRelation.setRole(UserClassRelation.RoleInClass.MEMBER); // 默认角色为成员
        newRelation.setStatus(UserClassRelation.JoinStatus.PENDING);
        newRelation.setJoinReason(joinRequestDto.getJoinReason());
        newRelation.setCreatedAt(LocalDateTime.now());

        userClassRelationRepository.save(newRelation);
    }

    /**
     * 获取班级的待审批申请列表
     * @param classId 班级ID
     * @param pageable 分页参数
     * @return 分页的待审批申请列表
     * @throws BusinessException 如果用户无权限或班级不存在
     */
    @Transactional(readOnly = true)
    public Page<ApprovalResponseDto> getApprovalList(Long classId, Pageable pageable) {
        // 获取当前登录用户
        User currentUser = userService.getCurrentUserEntity();

        // 检查班级是否存在
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("getApprovalList_FAILED", "班级不存在"));

        // 权限检查：验证当前用户是否是该班级的ADMIN或OWNER
        if (!hasClassAdminPermission(currentUser.getId(), classId)) {
            throw new BusinessException("PermissionDenied", "您没有权限查看该班级的申请列表");
        }

        // 分页查询PENDING状态的申请
        Page<UserClassRelation> pendingApplications =
                userClassRelationRepository.findByClassEntityAndStatus(
                        classEntity,
                        UserClassRelation.JoinStatus.PENDING,
                        pageable
                );

        // 转换为响应DTO
        return pendingApplications.map(this::convertToApprovalResponseDto);
    }

    /**
     * 处理申请审批
     * @param classId 班级ID
     * @param applicantUserId 申请人用户ID
     * @param approvalActionDto 审批操作DTO
     * @return 处理结果消息
     * @throws BusinessException 如果审批者无权限或申请不存在或班级不存在
     */
    public String processApproval(Long classId, Long applicantUserId, ApprovalActionDto approvalActionDto) {
        // 获取当前登录用户（审批者）
        User approver = userService.getCurrentUserEntity();

        // 检查班级是否存在
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("processApproval_FAILED", "班级不存在"));

        // 权限检查：验证审批者是否是ADMIN或OWNER
        if (!hasClassAdminPermission(approver.getId(), classId)) {
            throw new BusinessException("PermissionDenied", "您没有权限处理该班级的申请");
        }

        // 查找申请者和班级的PENDING状态关系记录
        UserClassRelation pendingRelation =
                userClassRelationRepository.findPendingApplicationByUserIdAndClassId(applicantUserId, classId)
                        .orElseThrow(() -> new BusinessException("processApproval_FAILED", "未找到待处理的申请记录"));

        // 根据action更新关系记录状态
        String resultMessage;
        if ("APPROVE".equals(approvalActionDto.getAction())) {
            // 批准申请
            pendingRelation.setStatus(UserClassRelation.JoinStatus.APPROVED);
            pendingRelation.setJoinedAt(LocalDateTime.now());
            pendingRelation.setApprovedBy(approver);
            resultMessage = "申请已批准";
        } else if ("REJECT".equals(approvalActionDto.getAction())) {
            // 拒绝申请
            pendingRelation.setStatus(UserClassRelation.JoinStatus.REJECTED);
            pendingRelation.setApprovedBy(approver);
            resultMessage = "申请已拒绝";
        } else {
            throw new BusinessException("processApproval_FAILED", "无效的审批操作");
        }

        // 更新审批时间
        pendingRelation.setUpdatedAt(LocalDateTime.now());
        userClassRelationRepository.save(pendingRelation);

        return resultMessage;
    }

    /**
     * 获取用户加入的所有班级
     * @param pageable 分页参数
     * @return 分页的班级列表
     */
    @Transactional(readOnly = true)
    public Page<ClassResponseDto> getMyClasses(Pageable pageable) {
        // 获取当前登录用户
        User currentUser = userService.getCurrentUserEntity();

        // 分页查询该用户所有APPROVED状态的班级关系记录
        Page<UserClassRelation> approvedRelations =
                userClassRelationRepository.findByUserAndStatus(
                        currentUser,
                        UserClassRelation.JoinStatus.APPROVED,
                        pageable
                );

        // 转换为ClassResponseDto
        return approvedRelations.map(relation -> convertToClassResponseDto(relation.getClassEntity()));
    }

    /**
     * 变更班级成员角色 - 由Controller调用，已通过@PreAuthorize确保操作者为Owner
     * @param classId 班级ID
     * @param userId 要变更角色的用户ID
     * @param newRole 新的角色枚举 (ADMIN 或 MEMBER)
     * @throws BusinessException 如果用户不存在、角色无效或尝试修改Owner
     */
    public void changeMemberRole(Long classId, Long userId, UserClassRelation.RoleInClass newRole) {
        // 获取当前用户（操作者）
        User currentOperator = userService.getCurrentUserEntity();

        // 查找要修改角色的用户与班级的关系记录
        UserClassRelation relation = userClassRelationRepository.findByUserIdAndClassEntityIdAndStatus(
                        userId, classId, UserClassRelation.JoinStatus.APPROVED)
                .orElseThrow(() -> new BusinessException("changeMemberRole_FAILED", "用户不是该班级的已批准成员"));

        // 安全检查：不允许修改创建者(Owner)的角色
        if (relation.getRole() == UserClassRelation.RoleInClass.OWNER) {
            throw new BusinessException("changeMemberRole_FAILED", "不能修改班级创建者的角色");
        }

        // 不允许创建者修改自己的角色（虽然理论上Owner不会走到这里）
        if (userId.equals(currentOperator.getId())) {
            throw new BusinessException("changeMemberRole_FAILED", "不能修改自己的角色");
        }


        // 只允许设置为ADMIN或MEMBER
        if (newRole != UserClassRelation.RoleInClass.ADMIN &&
                newRole != UserClassRelation.RoleInClass.MEMBER) {
            throw new BusinessException("changeMemberRole_FAILED", "只能设置角色为管理员或普通成员");
        }

        // 检查是否真的需要更新
        if (relation.getRole() == newRole) {
            return; // 角色相同，无需更新
        }

        // 更新角色
        relation.setRole(newRole);
        relation.setUpdatedAt(LocalDateTime.now());

        userClassRelationRepository.save(relation);
    }

    /**
     * 检查用户在指定班级的权限
     * @param classId 班级ID
     * @return 是否有管理权限
     */
    @Transactional(readOnly = true)
    public boolean checkClassPermission(Long classId) {
        User currentUser = userService.getCurrentUserEntity();
        return hasClassAdminPermission(currentUser.getId(), classId);
    }

    /**
     * 检查用户是否有班级管理权限
     * @param userId 用户ID
     * @param classId 班级ID
     * @return 是否有管理权限
     */
    @Transactional(readOnly = true)
    public boolean hasClassAdminPermission(Long userId, Long classId) {
        return userClassRelationRepository.hasAdminPermission(userId, classId);
    }

    /**
     * 生成唯一的邀请码
     * @return 8位随机邀请码
     */
    private String generateUniqueInviteCode() {
        String inviteCode;
        do {
            inviteCode = generateRandomString(INVITE_CODE_LENGTH);
        } while (classRepository.findByInviteCode(inviteCode).isPresent());

        return inviteCode;
    }

    /**
     * 生成指定长度的随机字符串
     * @param length 字符串长度
     * @return 随机字符串
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(INVITE_CODE_CHARS.length());
            sb.append(INVITE_CODE_CHARS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 将Classes实体转换为ClassResponseDto
     * @param classEntity 班级实体
     * @return 班级响应DTO
     */
    private ClassResponseDto convertToClassResponseDto(Classes classEntity) {
        return new ClassResponseDto(classEntity, classEntity.getOwner());
    }

    /**
     * 将UserClassRelation实体转换为ApprovalResponseDto
     * @param relation 用户班级关系实体
     * @return 审批响应DTO
     */
    private ApprovalResponseDto convertToApprovalResponseDto(UserClassRelation relation) {
        return new ApprovalResponseDto(relation);
    }
    /**
     * 获取班级成员列表 (分页)
     */
    @Transactional(readOnly = true)
    public Page<MemberResponseDto> getMemberList(Long classId, Pageable pageable) {
        // @PreAuthorize 已经保证了当前用户是成员，所以这里不再重复检查权限

        // 去关系表中，分页查询所有已批准的成员
        Page<UserClassRelation> relationPage = userClassRelationRepository
                .findByClassEntity_IdAndStatus(classId, UserClassRelation.JoinStatus.APPROVED, pageable);

        // 将查询结果 (Page<UserClassRelation>) 转换为 (Page<MemberResponseDto>)
        return relationPage.map(MemberResponseDto::new);
    }
    /**
     * 获取用户在指定班级中的角色和权限信息
     * @param classId 班级ID
     * @return 用户角色权限DTO
     */
    @Transactional(readOnly = true)
    public UserRolePermissionResponseDto getUserRoleInClass(Long classId) {
        // 获取当前登录用户
        User currentUser = userService.getCurrentUserEntity();

        // 检查班级是否存在
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("getUserRoleInClass_FAILED", "班级不存在"));

        // 查找用户与班级的关系记录
        Optional<UserClassRelation> relationOpt =
                userClassRelationRepository.findByUserAndClassEntity(currentUser, classEntity);

        // 如果没有找到关系记录或状态不是APPROVED，说明不是班级成员
        if (relationOpt.isEmpty() || relationOpt.get().getStatus() != UserClassRelation.JoinStatus.APPROVED) {
            return new UserRolePermissionResponseDto(); // 返回默认的非成员状态
        }

        // 根据关系记录构建权限DTO
        return new UserRolePermissionResponseDto(relationOpt.get());
    }
    /**
     * 根据班级ID获取邀请码
     * 权限检查由Controller层的@PreAuthorize负责
     * @param classId 班级ID
     * @return 包含邀请码的DTO
     */
    @Transactional(readOnly = true)
    public InviteCodeResponseDto getInviteCode(Long classId) {
        // 直接从数据库中查询班级实体
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("CLASS_NOT_FOUND", "班级不存在", 404));

        // 封装并返回邀请码
        return new InviteCodeResponseDto(classEntity.getInviteCode());
    }
    /**
     * 根据班级ID获取班级详情
     * 权限检查由Controller层的@PreAuthorize负责
     * @param classId 班级ID
     * @return 班级详情的响应DTO
     */
    @Transactional(readOnly = true)
    public ClassResponseDto getClassDetails(Long classId) {
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("CLASS_NOT_FOUND", "班级不存在", 404));

        // 使用已有的转换方法
        return convertToClassResponseDto(classEntity);
    }
    /**
     * 根据班级名称模糊搜索公开的班级
     * @param name 搜索关键字
     * @param pageable 分页参数
     * @return 分页的班级响应DTO
     */
    @Transactional(readOnly = true)
    public Page<ClassResponseDto> searchClassesByName(String name, Pageable pageable) {
        // 明确指定类型，调用Repository进行模糊查询
        Page<Classes> pageOfClasses = classRepository.findByNameContainingAndIsPublicTrue(name, pageable);

        // 将 Page<Classes> 转换为 Page<ClassResponseDto>
        return pageOfClasses.map(this::convertToClassResponseDto);
    }

}