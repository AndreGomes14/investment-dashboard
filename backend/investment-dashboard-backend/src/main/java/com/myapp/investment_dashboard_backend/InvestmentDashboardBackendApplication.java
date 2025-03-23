package com.myapp.investment_dashboard_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvestmentDashboardBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvestmentDashboardBackendApplication.class, args);
	}

}
