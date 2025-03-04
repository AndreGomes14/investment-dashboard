package com.myapp.investment_dashboard_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingDTO {
    private Long id;
    private Long userId;
    private String key;
    private String value;
}
