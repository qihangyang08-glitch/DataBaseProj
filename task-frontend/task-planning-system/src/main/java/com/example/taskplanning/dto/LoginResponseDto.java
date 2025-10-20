package com.example.taskplanning.dto;


/**
 * 用户登录响应DTO
 */
public class LoginResponseDto {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;

    // 无参构造函数
    public LoginResponseDto() {}

    // 全参构造函数
    public LoginResponseDto(String accessToken, String tokenType, Long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    // 静态工厂方法，方便创建标准的Bearer Token响应
    public static LoginResponseDto createBearerToken(String accessToken, Long expiresIn) {
        return new LoginResponseDto(accessToken, "Bearer", expiresIn);
    }

    // Getter和Setter方法
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public String toString() {
        return "LoginResponseDto{" +
                "accessToken='[PROTECTED]'" + // 出于安全考虑，不在日志中显示完整token
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }
}
