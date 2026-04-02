package com.example.strata.fee.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

data class FeeResponse(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val rate: BigDecimal,
    val country: String,
    val description: String
)

interface FeeCalculator {
    fun calculate(amount: BigDecimal): FeeResponse
}

@Service
class FeeCalculatorService(
    @Value("\${app.country:DEFAULT}") private val country: String
) : FeeCalculator {

    override fun calculate(amount: BigDecimal): FeeResponse {
        return when (country.uppercase()) {
            "US" -> calculateUS(amount)
            "EU" -> calculateEU(amount)
            "VN" -> calculateVN(amount)
            else -> calculateDefault(amount)
        }
    }

    private fun calculateUS(amount: BigDecimal): FeeResponse {
        // US: 0.5% flat rate, minimum $1
        val rate = BigDecimal("0.005")
        var fee = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        if (fee < BigDecimal.ONE) fee = BigDecimal.ONE
        return FeeResponse(amount, fee, rate, "US", "US flat rate 0.5%, min $1")
    }

    private fun calculateEU(amount: BigDecimal): FeeResponse {
        // EU: Tiered: 0.3% under €10,000, 0.1% above
        val threshold = BigDecimal("10000")
        val rate = if (amount < threshold) BigDecimal("0.003") else BigDecimal("0.001")
        val fee = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        return FeeResponse(amount, fee, rate, "EU", "EU tiered rate based on €10k threshold")
    }

    private fun calculateVN(amount: BigDecimal): FeeResponse {
        // VN: Fixed fee — 10,000 VND flat
        val fee = BigDecimal("10000")
        return FeeResponse(amount, fee, BigDecimal.ZERO, "VN", "VN fixed fee 10,000 VND")
    }

    private fun calculateDefault(amount: BigDecimal): FeeResponse {
        // Default: 1% flat rate
        val rate = BigDecimal("0.01")
        val fee = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        return FeeResponse(amount, fee, rate, "DEFAULT", "Default global rate 1%")
    }
}
