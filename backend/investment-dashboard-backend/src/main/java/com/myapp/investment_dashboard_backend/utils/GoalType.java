package com.myapp.investment_dashboard_backend.utils;

public enum GoalType {
    CAR("Car"),
    HOUSE("House"),
    MOTORBIKE("Motorbike"),
    VACATION("Vacation"),
    EMERGENCY_FUND("Emergency Fund"),
    EDUCATION("Education"),
    RETIREMENT("Retirement"),
    CASH_TARGET("Cash Target"),
    OTHER("Other");

    private final String displayName;

    GoalType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}