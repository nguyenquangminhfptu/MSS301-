package com.mss301.account.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest { @NotBlank private String username; @NotBlank private String password; }
