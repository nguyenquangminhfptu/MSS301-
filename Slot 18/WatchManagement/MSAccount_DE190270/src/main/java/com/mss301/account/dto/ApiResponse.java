package com.mss301.account.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> { private boolean success; private String message; private T data;
 public static <T> ApiResponse<T> success(T data,String message){return new ApiResponse<>(true,message,data);}
 public static <T> ApiResponse<T> error(String message){return new ApiResponse<>(false,message,null);}}
