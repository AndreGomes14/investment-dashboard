package com.myapp.investment_dashboard_backend.dto.portfolio;

import com.myapp.investment_dashboard_backend.model.Investment;
import java.util.List;

public record CategorizedInvestments(List<Investment> active, List<Investment> sold) {
} 