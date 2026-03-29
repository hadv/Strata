package com.example.strata.shared.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class AccountSummaryResponse(
    val accountId: UUID,
    val accountName: String,
    val balance: BigDecimal,
    val lastUpdated: Instant,
    val eventVersion: Long
)
