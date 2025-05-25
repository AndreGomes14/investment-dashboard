package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.dto.investment.InstrumentSearchResult;
import com.myapp.investment_dashboard_backend.dto.market_data.PriceInfo;

import java.io.IOException;
import java.util.List;

public interface MarketDataService {

    /**
     * Retrieves the current value and currency of a financial instrument.
     *
     * @param ticker The ticker symbol of the financial instrument.
     * @param type   The type of asset (e.g., "stock", "crypto").
     * @param targetCurrency The expected currency of the investment.
     * @return A PriceInfo object containing the value and currency, or null if the value cannot be retrieved.
     * @throws IOException if there is an error parsing the JSON response.
     */
    PriceInfo getCurrentValue(String ticker, String type, String targetCurrency) throws IOException;

    /**
     * Searches for financial instruments using external APIs.
     * @param query The search keyword string.
     * @return A list of matching instruments.
     */
    List<InstrumentSearchResult> searchInstruments(String query);
} 