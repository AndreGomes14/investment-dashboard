package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.ExternalApiCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalApiCacheRepository extends JpaRepository<ExternalApiCache, UUID> {
    Optional<ExternalApiCache> findByTickerAndType(String ticker, String type);

    @Query("SELECT DISTINCT e.ticker, e.type FROM ExternalApiCache e")
    List<Object[]> findDistinctTickersAndTypes();

    List<ExternalApiCache> findByLastUpdatedBefore(LocalDateTime cutoffDate);

    @Query("SELECT e FROM ExternalApiCache e WHERE e.ticker IN :tickers AND e.type = :type")
    List<ExternalApiCache> findByTickersAndType(List<String> tickers, String type);
}
