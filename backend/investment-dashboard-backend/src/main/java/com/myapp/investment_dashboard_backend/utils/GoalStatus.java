package com.myapp.investment_dashboard_backend.utils;

public enum GoalStatus {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    PAUSED("Paused"),
    CANCELLED("Cancelled");

    private final String displayName;

    GoalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}