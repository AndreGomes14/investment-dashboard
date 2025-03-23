package com.myapp.investment_dashboard_backend.dto.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Yahoo Finance API response
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooFinanceResponse {
    
    @JsonProperty("chart")
    private Chart chart;
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chart {
        
        @JsonProperty("result")
        private List<Result> result;
        
        @JsonProperty("error")
        private Error error;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        
        @JsonProperty("meta")
        private Meta meta;
        
        @JsonProperty("timestamp")
        private List<Long> timestamp;
        
        @JsonProperty("indicators")
        private Indicators indicators;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("symbol")
        private String symbol;
        
        @JsonProperty("regularMarketPrice")
        private Double regularMarketPrice;
        
        @JsonProperty("previousClose")
        private Double previousClose;
        
        @JsonProperty("gmtoffset")
        private Integer gmtOffset;
        
        @JsonProperty("exchangeName")
        private String exchangeName;
        
        @JsonProperty("regularMarketTime")
        private Long regularMarketTime;
        
        @JsonProperty("timezone")
        private String timezone;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Indicators {
        
        @JsonProperty("quote")
        private List<Map<String, List<Double>>> quote;
        
        @JsonProperty("adjclose")
        private List<Map<String, List<Double>>> adjclose;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("description")
        private String description;
    }
} 