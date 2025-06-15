package com.myapp.investment_dashboard_backend.scheduler;

import com.myapp.investment_dashboard_backend.service.impl.PortfolioServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for the investment dashboard.
 * Remember to add @EnableScheduling to a @Configuration class (e.g., the main application class)
 * for these scheduled tasks to be enabled.
 */
@Component
public class PortfolioUpdateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioUpdateScheduler.class);
    private final PortfolioServiceImpl portfolioService;

    @Autowired
    public PortfolioUpdateScheduler(PortfolioServiceImpl portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * Weekly job to update the current market values of all active investments
     * for all users and record their total portfolio values.
     * Runs every Sunday at 2:00 AM server time.
     * CRON: second, minute, hour, day of month, month, day(s) of week
     * Example: "0 0 2 ? * SUN" means "At 02:00:00 AM, on every Sunday"
     */
    @Scheduled(cron = "0 0 2 ? * SUN")
    public void runWeeklyPortfolioValueUpdates() {
        logger.info("Triggering weekly portfolio value updates for all users.");
        try {
            portfolioService.scheduledUpdateAllUsersPortfolioValues();
            logger.info("Weekly portfolio value updates completed successfully.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred during the scheduled weekly portfolio value updates.", e);
        }
    }
} 