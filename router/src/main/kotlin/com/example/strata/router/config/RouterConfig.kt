package com.example.strata.router.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "router")
class RouterConfig {
    var feeServiceUrls: Map<String, String> = mutableMapOf()
    var defaultFeeServiceUrl: String = "http://localhost:8090"
}
