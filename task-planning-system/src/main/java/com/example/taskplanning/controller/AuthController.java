// 7. 认证控制器 (AuthController.java)
package com.example.taskplanning.controller;

import com.example.taskplanning.dto.LoginRequestDto;
import com.example.taskplanning.dto.LoginResponseDto;
import lombok.extern.slf4j.Slf4j;

import com.example.taskplanning.ApiResponse;
import com.example.taskplanning.dto.UserRegistrationDto;
import com.example.taskplanning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.taskplanning.dto.ResendVerificationRequestDto;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody UserRegistrationDto registrationDto) {

        userService.registerUser(registrationDto);

        ApiResponse<Void> response = ApiResponse.success("注册成功");
        return ResponseEntity.ok(response);
    }
    /**
     * 用户登录
     *
     * @param loginRequest 登录请求数据
     * @return 登录结果（包含JWT Token）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> loginUser(
            @Valid @RequestBody LoginRequestDto loginRequest) {

        log.info("收到用户登录请求: username={}", loginRequest.getUsername());

        LoginResponseDto loginResponse = userService.loginUser(loginRequest);

        log.info("用户登录成功: username={}", loginRequest.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(loginResponse, "登录成功")
        );
    }
    /**
     * 邮箱验证接口
     * GET /api/auth/verify-email?token=...
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token);

        // 这里可以返回一个更友好的HTML页面，但为了API纯粹性，我们返回JSON
        return ResponseEntity.ok(ApiResponse.success("邮箱验证成功！您现在可以登录了。"));
    }
    /**
     * 重新发送验证邮件接口
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ResendVerificationRequestDto requestDto) {
        userService.resendVerificationEmail(requestDto);
        return ResponseEntity.ok(ApiResponse.success("新的验证邮件已发送，请检查您的收件箱。"));
    }
}

