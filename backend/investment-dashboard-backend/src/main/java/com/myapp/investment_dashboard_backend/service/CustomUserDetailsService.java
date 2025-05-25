package com.myapp.investment_dashboard_backend.service;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomUserDetailsService extends UserDetailsService {
    // No additional methods needed if it only serves to satisfy the UserDetailsService contract
    // and provide a type for injection.
    // The loadUserByUsername method is inherited from UserDetailsService.
} 