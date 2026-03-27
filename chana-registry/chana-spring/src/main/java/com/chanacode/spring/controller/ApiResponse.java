package com.chanacode.spring.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一 API 响应格式
 *
 * <p>所有 REST API 返回统一格式：
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {...}
 * }
 * </pre>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
