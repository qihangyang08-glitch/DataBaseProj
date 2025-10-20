// 3. 自定义业务异常 (BusinessException.java)
package com.example.taskplanning.exception;

public class BusinessException extends RuntimeException {

    private String errorCode;
    private int httpStatus;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 400;
    }

    public BusinessException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() { return errorCode; }
    public int getHttpStatus() { return httpStatus; }

    //
}
