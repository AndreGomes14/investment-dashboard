package com.myapp.investment_dashboard_backend.mapper;

import com.myapp.investment_dashboard_backend.dto.api.ExternalApiDataDTO;
import com.myapp.investment_dashboard_backend.model.ExternalApiCache;

import java.util.List;

public interface ExternalApiDataMapper {

    /* Mapping methods are retained for future use if needed. Implementation can be provided manually
       or by re-enabling MapStruct. */

    ExternalApiDataDTO toDto(ExternalApiCache cache);

    List<ExternalApiDataDTO> toDtoList(List<ExternalApiCache> caches);

    ExternalApiCache toEntity(ExternalApiDataDTO dto);
}
