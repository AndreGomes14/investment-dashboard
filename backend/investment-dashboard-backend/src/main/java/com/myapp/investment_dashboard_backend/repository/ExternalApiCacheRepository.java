package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalApiCacheRepository extends JpaRepository<ExternalApiCache, UUID> {
    Optional<ExternalApiCache> findByTickerAndType(String ticker, String type);
}
