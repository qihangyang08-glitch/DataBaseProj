// UserController.java (V1.0 - 全新创建)
package com.example.taskplanning.controller;

import com.example.taskplanning.ApiResponse;
import com.example.taskplanning.dto.UserResponseDto;
import com.example.taskplanning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users") // <-- 这个Controller负责所有 /api/users 开头的路径
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户的详细信息
     * GET /api/users/me
     *
     * @return 包含脱敏后用户信息的ApiResponse
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // 确保只有已登录的用户才能访问
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser() {
        // 调用Service层获取当前登录用户的DTO
        UserResponseDto currentUserDto = userService.getCurrentUserDto();

        // 使用ApiResponse包装成功响应
        return ResponseEntity.ok(ApiResponse.success(currentUserDto, "Successfully fetched current user data."));
    }

    // 未来你可以在这里添加其他用户相关的API，比如：
    // @PutMapping("/me")
    // public ResponseEntity<ApiResponse<UserResponseDto>> updateCurrentUser(...) { ... }

}
