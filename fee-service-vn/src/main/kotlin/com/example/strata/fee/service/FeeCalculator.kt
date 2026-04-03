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
        // VN: Fixed fee — 10,000 VND flat
        val fee = BigInteger.valueOf(10000)
        return FeeResponse(amount, fee, BigInteger.ZERO, "VN", "VN fixed fee 10,000")
    }
}
