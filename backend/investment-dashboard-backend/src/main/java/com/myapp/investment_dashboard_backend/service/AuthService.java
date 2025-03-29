package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.auth.AuthResponse;
import com.myapp.investment_dashboard_backend.dto.auth.LoginRequest;
import com.myapp.investment_dashboard_backend.dto.auth.RegisterRequest;
import com.myapp.investment_dashboard_backend.dto.user.UserDTO;
import com.myapp.investment_dashboard_backend.exception.ResourceAlreadyExistsException;
import com.myapp.investment_dashboard_backend.exception.ResourceNotFoundException;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import com.myapp.investment_dashboard_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUsername());
        UserDTO userDTO = createUserDTO(user);
        return createAuthResponse(token, userDTO);
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Find user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtTokenProvider.generateToken(user.getUsername());
        UserDTO userDTO = createUserDTO(user);
        return createAuthResponse(token, userDTO);
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserDTO createUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponse createAuthResponse(String token, UserDTO userDTO) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime() / 1000)
                .user(userDTO)
                .build();
    }
}