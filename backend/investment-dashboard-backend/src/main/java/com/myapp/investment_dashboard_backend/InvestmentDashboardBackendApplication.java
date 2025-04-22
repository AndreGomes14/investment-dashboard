package com.myapp.investment_dashboard_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class InvestmentDashboardBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvestmentDashboardBackendApplication.class, args);
	}

}
