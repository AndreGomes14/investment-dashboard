package com.myapp.investment_dashboard_backend.dto.auth;

import com.myapp.investment_dashboard_backend.dto.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private UserDTO user;
}
