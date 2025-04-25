package com.myapp.investment_dashboard_backend.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InstrumentSearchResult {
    private String symbol;
    private String name;
    private String type;
    private String region;
    private String currency;
}
