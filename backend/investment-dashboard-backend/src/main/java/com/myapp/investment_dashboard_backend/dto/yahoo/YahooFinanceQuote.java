package com.myapp.investment_dashboard_backend.dto.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Data Transfer Object for Yahoo Finance quote data
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooFinanceQuote {
    
    private String symbol;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal open;
    private BigDecimal previousClose;
    private Long volume;
    private LocalDateTime timestamp;
    private String currency;
    private String marketState; // e.g., "REGULAR", "PRE", "POST", "CLOSED"
    
    @JsonProperty("regularMarketPrice")
    private BigDecimal regularMarketPrice;
    
    @JsonProperty("regularMarketChange")
    private BigDecimal regularMarketChange;
    
    @JsonProperty("regularMarketChangePercent")
    private BigDecimal regularMarketChangePercent;
    
    @JsonProperty("regularMarketTime")
    private Long regularMarketTime;
    
    /**
     * Set the regular market time as a Unix timestamp and convert to LocalDateTime
     * @param unixTime The Unix timestamp in seconds
     */
    public void setRegularMarketTime(Long unixTime) {
        this.regularMarketTime = unixTime;
        if (unixTime != null) {
            this.timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(unixTime), 
                ZoneId.systemDefault()
            );
        }
    }
    
    /**
     * Set the primary price based on what's available
     * @param price The price value
     */
    @JsonProperty("price")
    public void setPrice(BigDecimal price) {
        this.price = price;
        if (this.regularMarketPrice == null) {
            this.regularMarketPrice = price;
        }
    }
    
    /**
     * Get the most appropriate price based on what's available
     * @return The current price
     */
    public BigDecimal getCurrentPrice() {
        return regularMarketPrice != null ? regularMarketPrice : price;
    }
} 