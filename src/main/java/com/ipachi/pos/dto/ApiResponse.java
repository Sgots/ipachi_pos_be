package com.ipachi.pos.dto;


        public class ApiResponse<T> {
    public int code;
    public String message;
    public T data;
    public ApiResponse(int code, String message, T data) {
                this.code = code; this.message = message; this.data = data;
            }
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "OK", data); }
    public static <T> ApiResponse<T> err(int code, String msg) { return new ApiResponse<>(code, msg, null); }
}
