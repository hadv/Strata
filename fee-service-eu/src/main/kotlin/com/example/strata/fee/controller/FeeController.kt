package com.example.strata.fee.controller

import com.example.strata.fee.service.FeeCalculatorService
import com.example.strata.fee.service.FeeResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger

data class FeeRequest(val amount: BigInteger)

@RestController
@RequestMapping("/api/fees")
class FeeController(private val feeCalculator: FeeCalculatorService) {

    @PostMapping("/calculate")
    fun calculate(@RequestBody request: FeeRequest): FeeResponse {
        return feeCalculator.calculate(request.amount)
    }
}
