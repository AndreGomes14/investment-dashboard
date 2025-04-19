package com.myapp.investment_dashboard_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingDTO {
    private UUID id;
    private UUID userId;
    private String key;
    private String value;
}
