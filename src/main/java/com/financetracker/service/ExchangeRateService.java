package com.financetracker.service;

// Reads values from application.properties / application-local.properties
import org.springframework.beans.factory.annotation.Value;

// Marks this as a Spring service so it can be injected elsewhere
import org.springframework.stereotype.Service;

// For throwing HTTP errors with the right status code
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

// Spring's simple HTTP client — already included with spring-boot-starter-web
import org.springframework.web.client.RestTemplate;

// Lombok — generates getters/setters for the inner class below
import lombok.Data;

// For the @JsonProperty annotation that maps JSON field names to Java field names
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;

// ExchangeRateService converts money from one currency to another.
//
// Example use case:
//   User logs an expense of 100 MAD (Moroccan Dirham).
//   Their base currency is CAD (Canadian Dollar).
//   We call the exchange rate API to get the MAD → CAD rate (e.g. 0.1372).
//   We store 13.72 CAD as the convertedAmount so budgets work in one currency.
//
// We use exchangerate-api.com (free tier, 1500 requests/month).
// The API key is stored in application-local.properties (not committed to GitHub).
@Service
public class ExchangeRateService {

    // The API key from application-local.properties
    // @Value reads it automatically at startup — never hardcode secrets in source code
    @Value("${app.exchange-rate.api-key}")
    private String apiKey;

    // Spring's built-in HTTP client — makes GET requests and parses JSON automatically
    // We create one instance here and reuse it for every call (efficient)
    private final RestTemplate restTemplate = new RestTemplate();

    // Converts an amount from one currency to another.
    //
    // Parameters:
    //   amount       — the original amount (e.g. 100.00)
    //   fromCurrency — the currency it's in right now (e.g. "MAD")
    //   toCurrency   — the currency to convert to (e.g. "CAD")
    //
    // Returns the converted amount rounded to 4 decimal places.
    // If both currencies are the same, returns the amount unchanged (no API call needed).
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {

        // If the user is already in their base currency, no conversion needed
        // e.g. Canadian user logs an expense in CAD — skip the API call
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        // Get the exchange rate (e.g. 1 MAD = 0.1372 CAD)
        BigDecimal rate = getRate(fromCurrency, toCurrency);

        // Multiply the amount by the rate to get the converted amount
        // HALF_UP rounding — standard rounding (4.5 → 5, 4.4 → 4)
        // 4 decimal places — enough precision for financial amounts
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    // Fetches the exchange rate from the API for a given currency pair.
    //
    // Uses the /pair endpoint: returns just one rate — simpler than downloading
    // all 160+ rates and searching through them.
    //
    // API URL format:
    //   https://v6.exchangerate-api.com/v6/{API_KEY}/pair/{FROM}/{TO}
    //
    // Example response JSON:
    //   { "result": "success", "base_code": "MAD", "target_code": "CAD", "conversion_rate": 0.1372 }
    public BigDecimal getRate(String fromCurrency, String toCurrency) {

        // Build the API URL with our key and the two currency codes
        String url = "https://v6.exchangerate-api.com/v6/" + apiKey
                + "/pair/" + fromCurrency.toUpperCase() + "/" + toCurrency.toUpperCase();

        // Make the GET request — RestTemplate fetches the URL and maps the JSON
        // response into an ExchangeRateResponse object automatically
        ExchangeRateResponse response;
        try {
            response = restTemplate.getForObject(url, ExchangeRateResponse.class);
        } catch (Exception e) {
            // Network error, timeout, or API is down — fail with a clear HTTP 503 error
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Could not reach exchange rate service. Try again later.");
        }

        // If the API returned null or a failure status, we can't proceed
        // "success" is the only valid result from this API
        if (response == null || !"success".equals(response.getResult())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid currency code. Use a valid ISO 4217 code like CAD, USD, MAD.");
        }

        // Return the rate as BigDecimal for precise money math
        // (double would introduce floating-point rounding errors)
        return BigDecimal.valueOf(response.getConversionRate());
    }

    // Inner class that maps the JSON response from exchangerate-api.com.
    // RestTemplate fills this in automatically using Jackson (JSON library).
    //
    // The API returns:
    //   { "result": "success", "conversion_rate": 0.1372, ... }
    //
    // We only need two fields, so we only declare those two.
    @Data // generates getters and setters so Jackson can fill the fields in
    static class ExchangeRateResponse {

        // "success" or "error" — we check this to know if the request worked
        private String result;

        // The actual exchange rate we need (e.g. 0.1372)
        // @JsonProperty maps the snake_case JSON name to our camelCase Java field
        @JsonProperty("conversion_rate")
        private double conversionRate;
    }
}
