package com.shopease.shopease.dto;

import com.shopease.shopease.entity.Role;

public record UserResponse(Long id, String name, String email, Role role) {}