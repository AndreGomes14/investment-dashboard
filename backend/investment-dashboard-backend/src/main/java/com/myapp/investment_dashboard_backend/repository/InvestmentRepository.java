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
    List<Investment> findByPortfolio(Portfolio portfolio);

    List<Investment> findByPortfolioAndStatus(Portfolio portfolio, String status);

    @Query("SELECT i FROM Investment i WHERE i.portfolio.id = :portfolioId AND i.status = 'ACTIVE'")
    List<Investment> findActiveInvestmentsByPortfolioId(UUID portfolioId);

    @Query("SELECT i FROM Investment i WHERE i.ticker = :ticker AND i.type = :type AND i.status = 'ACTIVE'")
    List<Investment> findActiveInvestmentsByTickerAndType(String ticker, String type);

    @Query("SELECT DISTINCT i.ticker, i.type FROM Investment i WHERE i.status = 'ACTIVE'")
    List<Object[]> findDistinctTickersAndTypesByActiveStatus();

    @Query("SELECT i FROM Investment i WHERE i.lastUpdateDate < :cutoffDate AND i.status = 'ACTIVE'")
    List<Investment> findStaleInvestments(LocalDateTime cutoffDate);

    @Query("SELECT i FROM Investment i WHERE i.portfolio.user.id = :userId AND i.status = 'ACTIVE'")
    List<Investment> findActiveInvestmentsByUserId(UUID userId);

    Optional<Investment> findByPortfolioAndTickerAndTypeAndStatus(
            Portfolio portfolio, String ticker, String type, String status);

    Optional<Investment> findById(UUID id);

    void deleteById(UUID id);

    List<Investment> findByPortfolioId(UUID portfolioId);

    List<Investment> findByPortfolio_IdIn(List<UUID> portfolioIds);

}
