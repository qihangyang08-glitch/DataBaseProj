package com.example.taskplanning.controller;

import com.example.taskplanning.ApiResponse;
import com.example.taskplanning.dto.SyncResultDto;
import com.example.taskplanning.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/sync")
@Tag(name = "智能同步", description = "智能同步相关接口")
public class SyncController {

    @Autowired
    private SyncService syncService;

    /**
     * 智能同步指定班级的任务到个人日历
     * @param classId 班级ID
     * @param range 时间范围 (day, week, month, semester, year)
     * @return 同步结果
     */
    @PostMapping("/class/{classId}")
    @PreAuthorize("@securityService.isClassMember(#classId)")
    @Operation(summary = "智能同步班级任务",
            description = "将指定班级在指定时间范围内的任务批量同步到用户的个人日历中")
    public ApiResponse<SyncResultDto> syncClassTasks(
            @PathVariable("classId")
            @Parameter(description = "班级ID", required = true)
            Long classId,

            @RequestParam("range")
            @NotBlank(message = "时间范围不能为空")
            @Pattern(regexp = "^(day|week|month|semester|year)$",
                    message = "时间范围只能是: day, week, month, semester, year")
            @Parameter(description = "时间范围", required = true,
                    example = "week")
            String range) {

        SyncResultDto result = syncService.syncClassTasks(classId, range);
        return ApiResponse.success(result, "成功同步 " + result.getNewlySyncedTasks() + " 个任务");
    }
}