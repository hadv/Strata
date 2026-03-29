package com.example.strata.shared.dto

import java.math.BigDecimal
import java.util.UUID

data class CreateAccountCommand(
    val accountId: UUID = UUID.randomUUID(),
    val accountName: String,
    val initialBalance: BigDecimal = BigDecimal.ZERO
)

data class DepositMoneyCommand(
    val accountId: UUID,
    val amount: BigDecimal,
    val description: String = ""
)

data class WithdrawMoneyCommand(
    val accountId: UUID,
    val amount: BigDecimal,
    val description: String = ""
)
