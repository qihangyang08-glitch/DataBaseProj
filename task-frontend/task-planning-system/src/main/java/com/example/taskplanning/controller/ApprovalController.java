package com.example.taskplanning.controller;

import com.example.taskplanning.ApiResponse;
import com.example.taskplanning.dto.GlobalApprovalResponseDto;
import com.example.taskplanning.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    @Autowired
    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /**
     * 获取当前用户所有管理班级中的待审批申请
     * GET /api/approvals/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<GlobalApprovalResponseDto>>> getGlobalPendingApprovals(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<GlobalApprovalResponseDto> approvals = approvalService.getGlobalPendingApprovals(pageable);

        return ResponseEntity.ok(ApiResponse.success(approvals, "成功获取待审批列表"));
    }
}
