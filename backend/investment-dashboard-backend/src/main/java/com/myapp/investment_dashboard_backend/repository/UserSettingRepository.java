package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.model.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {
    List<UserSetting> findByUser(User user);

    List<UserSetting> findByUserAndKeyStartingWith(User user, String keyPrefix);

    Optional<UserSetting> findByUserAndKey(User user, String key);
}