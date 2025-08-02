package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.FinancialProfile;
import com.myapp.investment_dashboard_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialProfileRepository extends JpaRepository<FinancialProfile, UUID> {
    Optional<FinancialProfile> findByUser(User user);
    Optional<FinancialProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}