package com.example.userservice.controller;

import com.example.userservice.dto.UserDto;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto.SignupResponse> signup(
            @Valid @RequestBody UserDto.SignupRequest request) {
        return ResponseEntity.ok(userService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto.LoginResponse> login(
        @Valid @RequestBody UserDto.LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto.MyInfoResponse> getMyInfo(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.getMyInfo(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto.MyInfoResponse> updateMyInfo(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(userService.updateMyInfo(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyInfo(
            @RequestHeader("X-User-Id") Long userId) {
        userService.deleteMyInfo(userId);
        return ResponseEntity.noContent().build();
    }
}
