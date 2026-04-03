package com.example.strata.fee.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigInteger

data class FeeResponse(
    val amount: BigInteger,
    val fee: BigInteger,
    val rate: BigInteger,
    val country: String,
    val description: String
)

interface FeeCalculator {
    fun calculate(amount: BigInteger): FeeResponse
}

@Service
class FeeCalculatorService : FeeCalculator {

    override fun calculate(amount: BigInteger): FeeResponse {
        // EU: Tiered: 0.3% (30 bps) under 10,000, 0.1% (10 bps) above
        val threshold = BigInteger.valueOf(10000)
        val rate = if (amount < threshold) BigInteger.valueOf(30) else BigInteger.valueOf(10)
        val fee = amount.multiply(rate).divide(BigInteger.valueOf(10000))
        return FeeResponse(amount, fee, rate, "EU", "EU tiered rate (30/10 bps) based on 10k threshold")
    }
}
