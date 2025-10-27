package com.finansal.finansal_deneme.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds external API settings from application.properties (IoC: externalized configuration).
 * Prefix: financial.api
 *  - url: Base URL for the financial data API
 *  - key: API key (can be empty in dev)
 */
@ConfigurationProperties(prefix = "financial.api")
public class FinancialApiProperties {
    /** Base URL of the financial API, e.g. https://www.alphavantage.co/query */
    private String url;
    /** API key; prefer environment injection for secrets */
    private String key;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
