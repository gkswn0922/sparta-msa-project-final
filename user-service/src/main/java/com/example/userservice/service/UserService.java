package com.example.userservice.service;

import com.example.userservice.config.JwtUtil;
import com.example.userservice.dto.UserDto;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    public UserDto.SignupResponse signup(UserDto.SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phone(request.getPhone())
            .build();

        User saved = userRepository.save(user);

        return UserDto.SignupResponse.builder()
            .id(saved.getId())
            .email(saved.getEmail())
            .name(saved.getName())
            .message("회원가입이 완료되었습니다")
            .build();
    }

    public UserDto.LoginResponse login(UserDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("탈퇴한 회원입니다");
        }

        String accessToken = jwtUtil.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
            "refreshToken:" + user.getId(),
            refreshToken,
            7, TimeUnit.DAYS
        );

        return UserDto.LoginResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
}
