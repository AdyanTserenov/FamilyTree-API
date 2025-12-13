package com.project.familytree.auth.dto;

public class CustomApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public CustomApiResponse() {
    }

    public CustomApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> CustomApiResponse<T> success(String message, T data) {
        return new CustomApiResponse<>(true, message, data);
    }

    public static <T> CustomApiResponse<T> successMessage(String message) {
        return new CustomApiResponse<>(true, message, null);
    }

    public static <T> CustomApiResponse<T> successData(T data) {
        return new CustomApiResponse<>(true, null, data);
    }

    public static <T> CustomApiResponse<T> error(String message) {
        return new CustomApiResponse<>(false, message, null);
    }
}