package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.Portfolio;
import com.myapp.investment_dashboard_backend.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.sound.sampled.Port;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.investments WHERE p.id = :id")
    Optional<Portfolio> findByIdWithInvestments(UUID id);

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.investments WHERE p.user.id = :userId")
    List<Portfolio> findByUserIdWithInvestments(UUID userId);

    @NotNull
    Optional<Portfolio> findById(@NotNull UUID portfolioId);

    List<Portfolio> findByUserId(UUID userId);
}