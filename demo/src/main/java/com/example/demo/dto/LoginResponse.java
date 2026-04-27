package com.example.demo.dto;

public record LoginResponse(Long userId, String username, String displayName, String role, String token) {
}
