package com.mss301.brand.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> {private boolean success; private String message; private T data; public static <T> ApiResponse<T> success(T data,String m){return new ApiResponse<>(true,m,data);} public static <T> ApiResponse<T> error(String m){return new ApiResponse<>(false,m,null);}}
