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
        // US: 0.5% flat rate (50 bps), minimum 1.00 (100 units)
        val rate = BigInteger.valueOf(50)
        var fee = amount.multiply(rate).divide(BigInteger.valueOf(10000))
        if (fee < BigInteger.valueOf(100)) fee = BigInteger.valueOf(100)
        return FeeResponse(amount, fee, rate, "US", "US flat rate 0.5% (50 bps), min 1.00")
    }
}
