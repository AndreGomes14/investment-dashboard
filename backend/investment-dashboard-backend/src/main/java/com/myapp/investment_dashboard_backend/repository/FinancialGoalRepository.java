package com.myapp.investment_dashboard_backend.repository;

import com.myapp.investment_dashboard_backend.model.FinancialGoal;
import com.myapp.investment_dashboard_backend.model.FinancialProfile;
import com.myapp.investment_dashboard_backend.utils.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {
    List<FinancialGoal> findByFinancialProfile(FinancialProfile financialProfile);
    
    List<FinancialGoal> findByFinancialProfileOrderByPriorityAscTargetDateAsc(FinancialProfile financialProfile);
    
    List<FinancialGoal> findByFinancialProfileAndStatus(FinancialProfile financialProfile, GoalStatus status);
    
    @Query("SELECT fg FROM FinancialGoal fg WHERE fg.financialProfile = :profile AND fg.targetDate <= :date AND fg.status = :status")
    List<FinancialGoal> findGoalsDueBefore(@Param("profile") FinancialProfile profile, 
                                          @Param("date") LocalDate date, 
                                          @Param("status") GoalStatus status);
    
    @Query("SELECT COUNT(fg) FROM FinancialGoal fg WHERE fg.financialProfile = :profile AND fg.status = :status")
    Long countByFinancialProfileAndStatus(@Param("profile") FinancialProfile profile, 
                                        @Param("status") GoalStatus status);
}