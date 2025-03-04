package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.user.UserDTO;
import com.myapp.investment_dashboard_backend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDTO toDto(User user);

    List<UserDTO> toDtoList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)  // Explicitly ignore
    @Mapping(target = "settings", ignore = true)
    @Mapping(target = "portfolios", ignore = true)
    User toEntity(UserDTO userDTO);
}