package com.myapp.investment_dashboard_backend.controller;

import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.model.UserSetting;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import com.myapp.investment_dashboard_backend.repository.UserSettingRepository;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;

    @PutMapping("/preferred-currency")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePreferredCurrency(@RequestBody Map<String, String> payload) {
        String currency = payload.getOrDefault("currency", "USD").toUpperCase(Locale.ROOT);
        if (!currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Invalid currency code");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));

        Optional<UserSetting> existingOpt = userSettingRepository.findByUserIdAndKey(user.getId(), "preferredCurrency");
        UserSetting setting = existingOpt.orElseGet(() -> {
            UserSetting s = new UserSetting();
            s.setUser(user);
            s.setKey("preferredCurrency");
            return s;
        });
        setting.setValue(currency);
        userSettingRepository.save(setting);
    }

    @PutMapping("/dark-mode")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDarkMode(@RequestBody Map<String, Object> payload) {
        Boolean darkMode = false;
        Object value = payload.get("darkMode");
        if (value instanceof Boolean boolVal) {
            darkMode = boolVal;
        } else if (value instanceof String strVal) {
            darkMode = Boolean.parseBoolean(strVal);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));

        Optional<UserSetting> existingOpt = userSettingRepository.findByUserIdAndKey(user.getId(), "darkMode");
        UserSetting setting = existingOpt.orElseGet(() -> {
            UserSetting s = new UserSetting();
            s.setUser(user);
            s.setKey("darkMode");
            return s;
        });
        setting.setValue(darkMode.toString());
        userSettingRepository.save(setting);
    }
} 