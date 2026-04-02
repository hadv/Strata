package com.example.strata.shared.dto

import java.math.BigInteger
import java.util.UUID

data class CreateAccountCommand(
    val accountId: UUID = UUID.randomUUID(),
    val accountName: String,
    val initialBalance: BigInteger = BigInteger.ZERO
)

data class DepositMoneyCommand(
    val accountId: UUID,
    val amount: BigInteger,
    val description: String = ""
)

data class WithdrawMoneyCommand(
    val accountId: UUID,
    val amount: BigInteger,
    val description: String = ""
)
