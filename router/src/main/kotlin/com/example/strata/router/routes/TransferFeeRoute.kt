package com.example.strata.router.routes

import com.example.strata.router.config.RouterConfig
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode
import org.springframework.stereotype.Component

data class FeeCalculationRequest(val amount: Double, val country: String)

@Component
class TransferFeeRoute(private val config: RouterConfig) : RouteBuilder() {

    override fun configure() {
        // Configure REST component
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true")

        // REST DSL for fee calculation
        rest("/transfer")
            .post("/calculate-fee")
            .type(FeeCalculationRequest::class.java)
            .to("direct:calculate-fee")

        // Route: Content-Based Router by country
        from("direct:calculate-fee")
            .routeId("transfer-fee-router")
            .log("Calculating fee for amount \${body.amount} in country \${body.country}")
            .setProperty("amount", simple("\${body.amount}"))
            .setProperty("country", simple("\${body.country}"))
            
            // Map country code to dynamic endpoint based on RouterConfig
            .choice()
                .`when`(simple("\${header.country} == 'US' or \${body.country} == 'US'"))
                    .toD("\${bean:routerConfig?method=getFeeServiceUrls().get('US')}/api/fees/calculate")
                .`when`(simple("\${header.country} == 'EU' or \${body.country} == 'EU'"))
                    .toD("\${bean:routerConfig?method=getFeeServiceUrls().get('EU')}/api/fees/calculate")
                .`when`(simple("\${header.country} == 'VN' or \${body.country} == 'VN'"))
                    .toD("\${bean:routerConfig?method=getFeeServiceUrls().get('VN')}/api/fees/calculate")
                .otherwise()
                    .toD("\${bean:routerConfig?method=getDefaultFeeServiceUrl()}/api/fees/calculate")
            .end()
            
            .log("Fee calculation response: \${body}")
            .process { exchange ->
                val body = exchange.getIn().getBody(Map::class.java)
                val amount = exchange.getProperty("amount", Double::class.java)
                val fee = (body["fee"] as Number).toDouble()
                
                val response = mutableMapOf<String, Any>()
                response.putAll(body as Map<String, Any>)
                response["totalCharged"] = amount + fee
                
                exchange.getIn().body = response
            }
    }
}
