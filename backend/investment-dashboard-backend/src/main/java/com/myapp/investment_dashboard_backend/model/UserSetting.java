package com.myapp.investment_dashboard_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "setting_key"})
})
@Data
@NoArgsConstructor
public class UserSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "setting_key", nullable = false, length = 50)
    private String key;

    @Column(name = "setting_value")
    private String value;

    public UserSetting(User user, String key, String value) {
        this.user = user;
        this.key = key;
        this.value = value;
    }
}
