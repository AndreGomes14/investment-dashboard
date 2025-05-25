package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.model.Investment;
import java.io.IOException;
import java.util.List;

public interface ExcelExportService {
    byte[] createInvestmentExcel(List<Investment> investments) throws IOException;
} 