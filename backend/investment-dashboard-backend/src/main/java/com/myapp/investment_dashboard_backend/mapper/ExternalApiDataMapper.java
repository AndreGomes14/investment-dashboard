package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.api.ExternalApiDataDTO;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ExternalApiDataMapper {

    ExternalApiDataDTO toDto(ExternalApiCache cache);

    List<ExternalApiDataDTO> toDtoList(List<ExternalApiCache> caches);

    @Mapping(target = "id", ignore = true)
    ExternalApiCache toEntity(ExternalApiDataDTO dto);
}
