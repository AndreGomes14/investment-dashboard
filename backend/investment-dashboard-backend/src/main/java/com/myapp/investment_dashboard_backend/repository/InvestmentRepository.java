package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, UUID> {
    Optional<Investment> findById(UUID id);

    List<Investment> findByPortfolioId(UUID portfolioId);

    List<Investment> findByPortfolio_IdIn(List<UUID> portfolioIds);
}
