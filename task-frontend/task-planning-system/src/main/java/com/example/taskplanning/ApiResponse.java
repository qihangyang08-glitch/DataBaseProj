// 2. 统一响应格式 (ApiResponse.java)
package com.example.taskplanning;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private String error;
    private int code;
    private LocalDateTime timestamp;

    // 私有构造函数
    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // 成功响应静态方法
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = message;
        response.code = 200;
        return response;
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    // 错误响应静态方法
    public static <T> ApiResponse<T> error(String error, String message, int code) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        response.message = message;
        response.code = code;
        return response;
    }

    // Getter和Setter方法
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
