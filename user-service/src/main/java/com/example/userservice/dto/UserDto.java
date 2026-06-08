package com.example.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public class UserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {

        @NotBlank
        @Email(message = "이메일 형식이 아닙니다")
        private String email;

        @NotBlank
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$",
            message = "비밀번호는 8자 이상, 숫자와 특수문자를 포함해야 합니다"
        )
        private String password;

        @NotBlank
        private String name;

        @NotBlank
        private String phone;
    }

    @Getter
    @Builder
    public static class SignupResponse {
        private Long id;
        private String email;
        private String name;
        private String message;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Getter
    @Builder
    public static class LoginResponse {
        private Long id;
        private String email;
        private String accessToken;
        private String refreshToken;
    }
}
