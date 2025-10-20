// 7. 全局异常处理器 (GlobalExceptionHandler.java)
package com.example.taskplanning.exception;

import com.example.taskplanning.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {} - {}", e.getErrorCode(), e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                e.getErrorCode(),
                e.getMessage(),
                e.getHttpStatus()
        );
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }

    /**
     * 处理数据校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(Exception e) {
        log.warn("数据校验异常: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();

        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            ex.getBindingResult().getAllErrors().forEach((error) -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            ex.getBindingResult().getAllErrors().forEach((error) -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
        }

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "VALIDATION_ERROR",
                "数据校验失败",
                400
        );
        response.setData(errors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理数据库约束异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("数据库约束异常: {}", e.getMessage());

        String message = "数据操作失败";
        String errorCode = "DATA_CONSTRAINT_VIOLATION";

        // 根据异常消息判断具体的约束违反类型
        if (e.getMessage().contains("username")) {
            message = "用户名已存在";
            errorCode = "USERNAME_EXISTS";
        } else if (e.getMessage().contains("email")) {
            message = "邮箱已被注册";
            errorCode = "EMAIL_EXISTS";
        } else if (e.getMessage().contains("phone")) {
            message = "手机号已被注册";
            errorCode = "PHONE_EXISTS";
        }

        ApiResponse<Void> response = ApiResponse.error(errorCode, message, 400);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    //@ResponseStatus(HttpStatus.FORBIDDEN) // <-- 返回 403
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        // 添加日志记录，便于调试
        log.error("检查班级管理权限时发生异常: {}", e.getMessage(), e);
        ApiResponse<Void> response = ApiResponse.error(
                "ACCESS_DENIED,",
                "e.getMessage()",
                403
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * 处理其他未预期的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("未处理的异常: {}", e.getMessage(), e);

        ApiResponse<Void> response = ApiResponse.error(
                "INTERNAL_ERROR",
                "服务器内部错误",
                500
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}