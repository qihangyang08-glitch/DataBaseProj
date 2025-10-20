// ClassController.java
package com.example.taskplanning.controller;

import com.example.taskplanning.dto.*;
import com.example.taskplanning.ApiResponse;
import com.example.taskplanning.entity.UserClassRelation;
import com.example.taskplanning.exception.BusinessException;
import com.example.taskplanning.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    @Autowired
    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * 创建班级
     * POST /api/classes
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ClassResponseDto>> createClass(
            @Valid @RequestBody ClassCreateDto createDto) {

        ClassResponseDto classResponse = classService.createClass(createDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(classResponse, "班级创建成功"));
    }

    /**
     * 根据邀请码搜索班级
     * GET /api/classes/search?inviteCode=ABC123XY
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ClassResponseDto>> findClassByInviteCode(
            @RequestParam("inviteCode") String inviteCode) {

        ClassResponseDto classResponse = classService.findClassByInviteCode(inviteCode);

        return ResponseEntity.ok(
                ApiResponse.success(classResponse, "班级查询成功")
        );
    }

    /**
     * 申请加入班级
     * POST /api/classes/{classId}/join
     */
    @PostMapping("/{classId}/join")
    public ResponseEntity<ApiResponse<String>> applyToJoinClass(
            @PathVariable Long classId,
            @Valid @RequestBody ClassJoinRequestDto joinRequestDto) {

        classService.applyToJoinClass(classId, joinRequestDto);

        return ResponseEntity.ok(
                ApiResponse.success("申请已提交，等待管理员审核", null)
        );
    }

    /**
     * 获取班级的待审批申请列表
     * GET /api/classes/{classId}/approvals
     * 需要ADMIN或OWNER权限
     */
    @GetMapping("/{classId}/approvals")
    @PreAuthorize("@classService.hasClassAdminPermission(authentication.principal.id, #classId)")
    public ResponseEntity<ApiResponse<Page<ApprovalResponseDto>>> getApprovalList(
            @PathVariable Long classId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<ApprovalResponseDto> approvalList = classService.getApprovalList(classId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(approvalList, "待审批列表查询成功")
        );
    }

    /**
     * 处理申请审批
     * PUT /api/classes/{classId}/approvals/{userId}
     * 需要ADMIN或OWNER权限
     */
    @PutMapping("/{classId}/approvals/{userId}")
    @PreAuthorize("@classService.hasClassAdminPermission(authentication.principal.id, #classId)")
    public ResponseEntity<ApiResponse<String>> processApproval(
            @PathVariable Long classId,
            @PathVariable Long userId,
            @Valid @RequestBody ApprovalActionDto approvalActionDto) {

        String resultMessage = classService.processApproval(classId, userId, approvalActionDto);

        return ResponseEntity.ok(
                ApiResponse.success(resultMessage, null)
        );
    }

    /**
     * 获取我加入的班级列表
     * GET /api/classes/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ClassResponseDto>>> getMyClasses(
            @PageableDefault(size = 20, sort = "joinedAt") Pageable pageable) {

        Page<ClassResponseDto> myClasses = classService.getMyClasses(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(myClasses, "我的班级列表查询成功")
        );
    }
    /**
     * 变更班级成员角色 - 由Controller调用，已通过@PreAuthorize确保操作者为Owner
     * @param classId 班级ID
     * @param userId 要变更角色的用户ID
     * @throws BusinessException 如果用户不存在、角色无效或尝试修改Owner
     */
    @PutMapping("/{classId}/members/{userId}/promote") //任命为管理员
    @PreAuthorize("@securityService.isClassOwner(#classId)")
    public ResponseEntity<ApiResponse<String>> promoteMemberToAdmin(
            @PathVariable Long classId,
            @PathVariable Long userId) {

        // 2. 将具体角色作为参数，而不是DTO，传递给Service
        classService.changeMemberRole(classId, userId, UserClassRelation.RoleInClass.ADMIN);

        // 3. 不再需要try-catch，返回标准的ApiResponse
        return ResponseEntity.ok(ApiResponse.success("成员已成功提升为管理员", null));
    }

    @PutMapping("/{classId}/members/{userId}/demote") // 剥夺管理员
    @PreAuthorize("@securityService.isClassOwner(#classId)")
    public ResponseEntity<ApiResponse<String>> demoteAdminToMember(
            @PathVariable Long classId,
            @PathVariable Long userId) {

        classService.changeMemberRole(classId, userId, UserClassRelation.RoleInClass.MEMBER);

        return ResponseEntity.ok(ApiResponse.success("管理员已成功降级为成员", null));
    }

    /**
     * 检查用户在指定班级的权限
     * GET /api/classes/{classId}/permission
     * 这个接口用于前端判断用户是否有管理权限，决定是否显示管理功能
     */
    @GetMapping("/{classId}/permission")
    public ResponseEntity<ApiResponse<Boolean>> checkClassPermission(
            @PathVariable Long classId) {

        boolean hasPermission = classService.checkClassPermission(classId);

        return ResponseEntity.ok(
                ApiResponse.success(hasPermission, "权限检查完成")
        );
    }
    /**
     * 获取指定班级的成员列表 (分页)
     * 只有班级成员才能查看
     *
     * @param classId 班级ID
     * @param pageable 分页参数
     * @return 分页的成员列表
     */
    @GetMapping("/{classId}/members") // <-- 1. 核心: 匹配 GET /api/classes/{classId}/members
    @PreAuthorize("@securityService.isClassMember(#classId)") // <-- 2. 权限: 只有班级成员才能看
    public ResponseEntity<ApiResponse<Page<MemberResponseDto>>> getMemberList(
            @PathVariable Long classId,
            @PageableDefault(size = 20, sort = "joinedAt") Pageable pageable) {

        // 3. 调用Service层完成业务逻辑
        Page<MemberResponseDto> memberPage = classService.getMemberList(classId, pageable);

        return ResponseEntity.ok(ApiResponse.success(memberPage, "成功获取班级成员列表"));
    }
    /**
     * 获取当前用户在指定班级中的角色和权限信息
     * GET /api/classes/{classId}/my-role
     * 支持前端基于角色的UI条件渲染
     */
    @GetMapping("/{classId}/my-role")
    @Operation(summary = "获取我在班级中的角色权限",
            description = "获取当前登录用户在指定班级中的具体角色和权限信息，用于前端UI条件渲染")
    public ResponseEntity<ApiResponse<UserRolePermissionResponseDto>> getMyRoleInClass(
            @PathVariable Long classId) {

        UserRolePermissionResponseDto rolePermission = classService.getUserRoleInClass(classId);

        return ResponseEntity.ok(
                ApiResponse.success(rolePermission, "角色权限查询成功")
        );
    }
    /**
     * 获取指定班级的邀请码
     * GET /api/classes/{classId}/invite-code
     * 只有班级成员才能获取
     */
    @GetMapping("/{classId}/invite-code")
    @PreAuthorize("@securityService.isClassMember(#classId)")
    public ResponseEntity<ApiResponse<InviteCodeResponseDto>> getInviteCode(@PathVariable Long classId) {
        InviteCodeResponseDto inviteCodeDto = classService.getInviteCode(classId);
        return ResponseEntity.ok(ApiResponse.success(inviteCodeDto, "成功获取班级邀请码"));
    }
    /**
     * 获取指定班级的详细信息
     * GET /api/classes/{classId}
     * 只有班级成员才能查看
     */
    @GetMapping("/{classId}")
    @PreAuthorize("@securityService.isClassMember(#classId)")
    public ResponseEntity<ApiResponse<ClassResponseDto>> getClassDetails(@PathVariable Long classId) {
        ClassResponseDto classDetails = classService.getClassDetails(classId);
        return ResponseEntity.ok(ApiResponse.success(classDetails, "成功获取班级详情"));
    }

}