// UserService.java (V2.0 - 重构版)
package com.example.taskplanning.service;

import com.example.taskplanning.JwtTokenProvider;
import com.example.taskplanning.config.CustomUserDetails;
import com.example.taskplanning.dto.*;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.exception.BusinessException;
import com.example.taskplanning.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.taskplanning.annotation.LogAction;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class UserService implements UserDetailsService { // <-- 1. 实现 UserDetailsService 接口

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // 使用构造函数注入，这是Spring推荐的最佳实践
    @Autowired
    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder, // 使用@Lazy解决潜在的循环依赖
                       JwtTokenProvider jwtTokenProvider,
                       @Lazy AuthenticationManager authenticationManager, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    /**
     * 【Spring Security 核心方法】
     * 根据用户名加载用户详情，用于认证过程
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // 返回自定义的CustomUserDetails对象，包含用户ID
        return new CustomUserDetails(
                user.getId(),           // 用户ID - 供@PreAuthorize使用
                user.getUsername(),     // 用户名
                user.getPassword(),     // 密码
                Collections.singletonList(new SimpleGrantedAuthority("USER")) // 基础权限
        );
    }

    /**
     * 用户注册
     */
    @Transactional
    @LogAction(action = "USER_REGISTER", entityType = "USER")
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new BusinessException("USERNAME_EXISTS", "用户名已存在");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setName(registrationDto.getName());
        user.setPhone(registrationDto.getPhone());

        // --- 新增逻辑 ---
        user.setEmailVerified(false); // 默认未验证
        String token = java.util.UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // 24小时后过期
        // --- 结束 ---

        User savedUser = userRepository.save(user);

        // --- 新增逻辑：发送验证邮件 ---
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getVerificationToken());
        // --- 结束 ---

        return convertToResponseDto(savedUser);
    }

    /**
     * 用户登录
     */
    @Transactional
    @LogAction(action = "USER_LOGIN", entityType = "USER")
    public LoginResponseDto loginUser(LoginRequestDto loginRequest) {
        // 1. 使用AuthenticationManager进行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. 将认证信息存入SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. 生成JWT Token
        String token = jwtTokenProvider.generateToken(authentication);

        // 4. 更新最后登录时间
        User user = userRepository.findByUsername(loginRequest.getUsername()).get();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return new LoginResponseDto(token, "Bearer", jwtTokenProvider.getJwtExpirationInMs());
    }
    /**
     * 验证用户邮箱
     * @param token 验证令牌
     */
    @Transactional
    public void verifyEmail(String token) {
        // 1. 根据令牌查找用户
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "无效的验证链接", 400));

        // 2. 检查令牌是否已过期
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("EXPIRED_TOKEN", "验证链接已过期，请重新申请", 400);
        }

        // 3. 更新用户状态
        user.setEmailVerified(true);
        user.setVerificationToken(null); // 清除令牌
        user.setVerificationTokenExpiry(null); // 清除过期时间

        userRepository.save(user);
    }
    /**
     * 重新发送验证邮件
     * @param requestDto 包含邮箱的请求DTO
     */
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequestDto requestDto) {
        // 1. 根据邮箱查找用户
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "该邮箱未注册", 404));

        // 2. 检查用户是否已经验证
        if (user.isEmailVerified()) {
            throw new BusinessException("ALREADY_VERIFIED", "该邮箱已经验证，请直接登录", 400);
        }

        // 3. 生成新的令牌和过期时间
        String token = java.util.UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(24));

        // 4. 保存更新后的用户信息
        userRepository.save(user);

        // 5. 异步发送新的验证邮件
        emailService.sendVerificationEmail(user.getEmail(), token);
    }


    /**
     * 获取当前已认证用户的 User 实体对象 (供内部Service调用)
     */
    public User getCurrentUserEntity() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database: " + username));
    }

    /**
     * 获取当前已认证用户的 DTO 对象 (供Controller调用)
     */
    public UserResponseDto getCurrentUserDto() {
        return convertToResponseDto(getCurrentUserEntity());
    }

    /**
     * 将User实体转换为UserResponseDto
     */
    private UserResponseDto convertToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}