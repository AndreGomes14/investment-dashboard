package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.auth.AuthResponse;
import com.myapp.investment_dashboard_backend.dto.auth.LoginRequest;
import com.myapp.investment_dashboard_backend.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
} 