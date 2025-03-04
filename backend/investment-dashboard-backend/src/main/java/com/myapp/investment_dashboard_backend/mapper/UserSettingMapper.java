package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.user.UserSettingDTO;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.model.UserSetting;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class UserSettingMapper {

    protected UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Mapping(source = "user.id", target = "userId")
    public abstract UserSettingDTO toDto(UserSetting userSetting);

    public abstract List<UserSettingDTO> toDtoList(List<UserSetting> userSettings);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    public abstract UserSetting toEntity(UserSettingDTO userSettingDTO);

    @AfterMapping
    protected void mapUser(UserSettingDTO dto, @MappingTarget UserSetting entity) {
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + dto.getUserId()));
            entity.setUser(user);
        }
    }
}