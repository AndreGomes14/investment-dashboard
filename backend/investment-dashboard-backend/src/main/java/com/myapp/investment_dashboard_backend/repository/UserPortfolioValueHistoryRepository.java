package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.UserPortfolioValueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserPortfolioValueHistoryRepository extends JpaRepository<UserPortfolioValueHistory, UUID> {

    List<UserPortfolioValueHistory> findByUserIdAndTimestampGreaterThanEqualOrderByTimestampAsc(UUID userId, LocalDateTime startDateTime);

    List<UserPortfolioValueHistory> findByUserIdOrderByTimestampAsc(UUID userId);

} 